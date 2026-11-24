package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.api.event.ClientVehicleFireEvent
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.util.concurrent.ThreadLocalRandom

/**
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object VehicleLightingHandler {

    /**
     * Subscribed to [ClientVehicleFireEvent] on the Forge event bus.
     * Generates punchy, high-visibility muzzle flashes tailored specifically for vehicles.
     *
     * @param event client-side vehicle fire event
     */
    @JvmStatic
    fun onVehicleFire(event: ClientVehicleFireEvent) {
        val vehicle: VehicleEntity = event.vehicle
        if (!vehicle.level().isClientSide) return

        val weaponName = event.weaponName
        val shooter = event.shooter

        val gunData = if (!weaponName.isNullOrEmpty()) {
            vehicle.getGunData(weaponName)
        } else {
            vehicle.getGunData(event.index)
        } ?: return

        val shootPos = vehicle.getShootPos(shooter, 1f)
        var shootVec = vehicle.getShootVec(shooter, 1f)

        if (shootVec.lengthSqr() < 1e-4) {
            shootVec = shooter.lookAngle
        }
        if (shootVec.lengthSqr() < 1e-4) return

        val damage = gunData.get(GunProp.DAMAGE)

        val maxLevel: Int
        val duration: Int
        when {
            damage >= 30.0 -> { maxLevel = 15; duration = 1 }
            damage >= 12.0 -> { maxLevel = 14; duration = 1 }
            else -> { maxLevel = 13; duration = 1 }
        }


        val dir = shootVec.normalize()
        val muzzlePos = shootPos.add(dir.scale(0.5))
        val strength = (maxLevel / 15f).coerceIn(0.35f, 2f)
        val radius = (6f + maxLevel * 0.45f).coerceIn(6f, 16f)
        WorldLightHandler.put(
            "vehicle_muzzle_${vehicle.id}_${event.index}", muzzlePos, Vec3.ZERO,
            strength = strength, radius = radius, durationTicks = duration,
            color = WorldLightHandler.warmColor(),
        )
    }

    /**
     * 受损/燃烧载具的火光：
     * 1. 弹药殉爆：3 个同时燃烧点（泄压板、驾驶舱盖、地面油污）；
     * 2. 残骸车体：稳定的余烬光，每 3 tick 更新一次；
     * 3. 中等损伤发动机起火：较暗的发动机舱火光。
     *
     * @param vehicle the vehicle entity being processed
     */
    @JvmStatic
    fun handleVehicleFireLight(vehicle: VehicleEntity) {
        val level = vehicle.level()
        if (!level.isClientSide) return
        if (!vehicle.data().compute().hasLowHealthWarning) return

        val tick = vehicle.tickCount
        val maxHealth = vehicle.getMaxHealth()
        val healthRatio = (vehicle.health / maxHealth).coerceIn(0f, 1f)
        val random = ThreadLocalRandom.current()

        val forward = vehicle.lookAngle.normalize()
        val right = vehicle.getRightVec(1f).normalize()
        val halfLen = (vehicle.bbWidth * 0.5).coerceAtLeast(1.2)

        // 1. 弹药殉爆 / 炮塔燃烧：三个同时燃烧点
        if (vehicle.turretBurnTimer > 0 && !vehicle.sympatheticDetonated) {
            // A: 泄压板 / 炮塔环主火舌
            val burnPos = vehicle.turretBurnEffectPos() ?: vehicle.position().add(0.0, vehicle.bbHeight * 0.7, 0.0)
            val maxLvlA = (14 + random.nextInt(2)).coerceIn(14, 15)
            WorldLightHandler.put(
                "vehicle_cook_a_${vehicle.id}", burnPos, Vec3.ZERO,
                (maxLvlA / 15f).coerceIn(0.2f, 1f), 3.2f, durationTicks = 3,
                color = WorldLightHandler.warmColor(),
            )

            // B: 驾驶舱盖 / 前车体通风口
            val hatchPos = vehicle.position().add(forward.scale(halfLen * 0.4)).add(0.0, vehicle.bbHeight * 0.6, 0.0)
            val maxLvlB = (12 + random.nextInt(3)).coerceIn(12, 14)
            WorldLightHandler.put(
                "vehicle_cook_b_${vehicle.id}", hatchPos, Vec3.ZERO,
                (maxLvlB / 15f).coerceIn(0.2f, 1f), 2.6f, durationTicks = 3,
                color = WorldLightHandler.warmColor(),
            )

            // C: 车体下方泄漏燃油
            val groundPos = vehicle.position().subtract(right.scale(vehicle.bbWidth * 0.4)).add(0.0, 0.2, 0.0)
            val maxLvlC = (10 + random.nextInt(3)).coerceIn(10, 12)
            WorldLightHandler.put(
                "vehicle_cook_c_${vehicle.id}", groundPos, Vec3.ZERO,
                (maxLvlC / 15f).coerceIn(0.15f, 1f), 2.0f, durationTicks = 3,
                color = WorldLightHandler.warmColor(),
            )
            return
        }

        // 2. 残骸车体：稳定余烬光
        if (vehicle.isWreck) {
            if (tick % 3 == 0) {
                val isChokedBySmoke = random.nextFloat() < 0.05f
                val maxLvl = if (isChokedBySmoke) 6 else (8 + random.nextInt(3))
                val isRear = random.nextBoolean()
                val pos = if (isRear) {
                    vehicle.position().subtract(forward.scale(halfLen * 0.5)).add(0.0, vehicle.bbHeight * 0.5, 0.0)
                } else {
                    vehicle.position().add(0.0, vehicle.bbHeight * 0.5, 0.0)
                }
                WorldLightHandler.put(
                    "vehicle_wreck_${vehicle.id}", pos, Vec3.ZERO,
                    (maxLvl / 15f).coerceIn(0.1f, 1f), 2.8f, durationTicks = 6,
                    color = WorldLightHandler.warmColor(),
                )
            }
            return
        }

        // 3. 中等损伤发动机起火
        if (healthRatio in 0.15f..0.50f && tick % 2 == 0) {
            val enginePos = vehicle.position()
                .subtract(forward.scale(halfLen * 0.4))
                .add(0.0, vehicle.bbHeight * 0.6, 0.0)
            WorldLightHandler.put(
                "vehicle_engine_fire_${vehicle.id}", enginePos, Vec3.ZERO,
                (6 / 15f).coerceIn(0.05f, 1f), 2.2f, durationTicks = 5,
                color = WorldLightHandler.warmColor(),
            )
        }
    }

    /**
     * 炮塔残骸的火焰光。
     *
     * @param wreck the turret wreck entity
     */
    @JvmStatic
    fun handleTurretWreckLight(wreck: TurretWreckEntity) {
        val level = wreck.level()
        if (!level.isClientSide) return
        if (wreck.tickCount % 2 != 0) return

        val player = Minecraft.getInstance().player ?: return
        if (player.distanceToSqr(wreck.x, wreck.y, wreck.z) > 192.0 * 192.0) return

        val random = ThreadLocalRandom.current()
        val isChoked = random.nextFloat() < 0.10f
        val maxLvl = if (isChoked) 4 else (7 + random.nextInt(2))

        val pos = Vec3(wreck.x, wreck.y + wreck.bbHeight * 0.5, wreck.z)
        WorldLightHandler.put(
            "turret_wreck_${wreck.id}", pos, Vec3.ZERO,
            (maxLvl / 15f).coerceIn(0.05f, 1f), 2.4f, durationTicks = 4,
            color = WorldLightHandler.warmColor(),
        )
    }

    /**
     * Emits an explosion flash when a turret wreck detonates upon destruction.
     *
     * @param wreck the turret wreck entity
     */
    @JvmStatic
    fun handleTurretWreckExplosion(wreck: TurretWreckEntity) {
        val level = wreck.level()
        if (!level.isClientSide) return

        val center = Vec3(wreck.x, wreck.y + wreck.bbHeight * 0.5, wreck.z)
        ProjectileLightHelper.emitExplosionFlashDirect(level, center, 5f)
    }

    /**
     * 载具被摧毁时的爆炸闪光。
     *
     * @param vehicle      the vehicle that exploded
     * @param customRadius optional explosion radius override in blocks
     */
    @JvmStatic
    fun emitVehicleExplosionLight(vehicle: VehicleEntity, customRadius: Float = 0f) {
        val level = vehicle.level()
        if (!level.isClientSide) return

        val destroyRadius = vehicle.computed().destroyInfo.explosionRadius
        val baseRadius = if (customRadius > 0f) customRadius else if (destroyRadius > 0f) destroyRadius else 6f
        val sizeFactor = ((vehicle.bbWidth * vehicle.bbHeight) / 3.0f).coerceIn(1.0f, 2.5f)
        val center = Vec3(vehicle.x, vehicle.y + vehicle.bbHeight * 0.5, vehicle.z)

        ProjectileLightHelper.emitExplosionFlashDirect(level, center, baseRadius * sizeFactor)
    }
}
