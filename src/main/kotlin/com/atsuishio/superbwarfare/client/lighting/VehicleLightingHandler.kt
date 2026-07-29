package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.api.event.ClientVehicleFireEvent
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.concurrent.ThreadLocalRandom

/**
 * Client-side handler for vehicle dynamic lighting events including weapon fire,
 * hull/turret burning, turret wreck illumination, and destruction explosions.
 *
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
        } ?: run {
            return
        }

        val shootPos = vehicle.getShootPos(shooter, 1f)
        var shootVec = vehicle.getShootVec(shooter, 1f)

        if (shootVec.lengthSqr() < 1e-4) {
            shootVec = shooter.lookAngle
        }

        if (shootVec.lengthSqr() < 1e-4) {
            return
        }

        val damage = gunData.get(GunProp.DAMAGE)

        val maxLevel: Int
        val minLevel: Int
        val duration: Int

        when {
            damage >= 30.0 -> { maxLevel = 15; minLevel = 11; duration = 4 }
            damage >= 12.0 -> { maxLevel = 14; minLevel = 9;  duration = 4 }
            else           -> { maxLevel = 13; minLevel = 8;  duration = 3 }
        }

        val params = MuzzleFlashHelper.FlashParams(maxLevel, minLevel, duration)
        MuzzleFlashHelper.spawnFlashCone(shootPos, shootVec, params)

        val engine = vehicle.level().lightEngine

        val dir = shootVec.normalize()
        val mountPos = shootPos.subtract(dir.scale(0.8))
        val mountBp = BlockPos.containing(mountPos.x, mountPos.y, mountPos.z)
        LightPositionRegistry.putSparkRadius(mountBp, (maxLevel - 2).coerceAtLeast(8), 5, duration, radius = 1)
        engine.checkBlock(mountBp)

        val centerBp = BlockPos.containing(vehicle.x, vehicle.y + vehicle.bbHeight * 0.5, vehicle.z)
        LightPositionRegistry.putSparkRadius(centerBp, maxLevel, minLevel, duration, radius = 2)
        engine.checkBlock(centerBp)
    }

    /**
     * Emits controlled, atmospheric fire light for damaged and burning vehicles.
     *
     * Features:
     * 1. Ammo Cook-off: 3 SIMULTANEOUS independent fire points (blowout panels, driver hatch, ground spill).
     * 2. Wreck Hull: Stable even ember glow (levels 8..10), updated every 3 ticks.
     * 3. Engine Fire: Dim engine compartment glow (level 6).
     *
     * @param vehicle the vehicle entity being processed
     */
    @JvmStatic
    fun handleVehicleFireLight(vehicle: VehicleEntity) {
        val level = vehicle.level()
        if (!level.isClientSide) return

        if (!vehicle.data().compute().hasLowHealthWarning) return

        val tick = vehicle.tickCount
        val engine = level.lightEngine
        val maxHealth = vehicle.getMaxHealth()
        val healthRatio = (vehicle.health / maxHealth).coerceIn(0f, 1f)
        
        val random = ThreadLocalRandom.current()

        val forward = vehicle.lookAngle.normalize()
        val right = vehicle.getRightVec(1f).normalize()
        val halfLen = (vehicle.bbWidth * 0.5).coerceAtLeast(1.2)

        // 1. Ammo Cook-off / Turret Burn: 3 SIMULTANEOUS roaring fire points (blowout panels, driver hatch, ground spill)
        if (vehicle.turretBurnTimer > 0 && !vehicle.sympatheticDetonated) {
            // Point A: Primary Jet Flame (Blowout Panels / Turret Ring)
            val burnPos = vehicle.turretBurnEffectPos() ?: vehicle.position().add(0.0, vehicle.bbHeight * 0.7, 0.0)
            val bpA = BlockPos.containing(burnPos.x, burnPos.y, burnPos.z)
            val maxLvlA = (14 + random.nextInt(2)).coerceIn(14, 15)
            LightPositionRegistry.putSparkRadius(bpA, maxLvlA, 10, ttlTicks = 3, radius = 2)
            engine.checkBlock(bpA)

            // Point B: Driver Hatch / Front Hull Vent
            val hatchPos = vehicle.position().add(forward.scale(halfLen * 0.4)).add(0.0, vehicle.bbHeight * 0.6, 0.0)
            val bpB = BlockPos.containing(hatchPos.x, hatchPos.y, hatchPos.z)
            val maxLvlB = (12 + random.nextInt(3)).coerceIn(12, 14)
            LightPositionRegistry.putSparkRadius(bpB, maxLvlB, 8, ttlTicks = 3, radius = 1)
            engine.checkBlock(bpB)

            // Point C: Spilled Fuel on Ground (Under Hull)
            val groundPos = vehicle.position().subtract(right.scale(vehicle.bbWidth * 0.4)).add(0.0, 0.2, 0.0)
            val bpC = BlockPos.containing(groundPos.x, groundPos.y, groundPos.z)
            val maxLvlC = (10 + random.nextInt(3)).coerceIn(10, 12)
            LightPositionRegistry.putSpark(bpC.asLong(), maxLvlC, 6, ttlTicks = 3)
            engine.checkBlock(bpC)
            return
        }

        // 2. Destroyed Wreck Hull: Stable ember glow, updated every 3 ticks (33% fewer ticks)
        if (vehicle.isWreck) {
            if (tick % 3 == 0) {
                val isChokedBySmoke = random.nextFloat() < 0.05f
                val maxLvl = if (isChokedBySmoke) 6 else (8 + random.nextInt(3)) // 8..10
                val minLvl = 5
                val ttl = 6

                val isRear = random.nextBoolean()
                val pos = if (isRear) {
                    vehicle.position().subtract(forward.scale(halfLen * 0.5)).add(0.0, vehicle.bbHeight * 0.5, 0.0)
                } else {
                    vehicle.position().add(0.0, vehicle.bbHeight * 0.5, 0.0)
                }

                val bp = BlockPos.containing(pos.x, pos.y, pos.z)
                LightPositionRegistry.putSparkRadius(bp, maxLvl, minLvl, ttlTicks = ttl, radius = 1)
                engine.checkBlock(bp)
            }
            return
        }

        // 3. Medium Damage Engine Fire (health 15% - 50%: dim engine glow, level 6, updated every 2 ticks)
        if (healthRatio in 0.15f..0.50f && tick % 2 == 0) {
            val maxLvl = 6
            val minLvl = 3
            val ttl = 5

            val enginePos = vehicle.position()
                .subtract(forward.scale(halfLen * 0.4))
                .add(0.0, vehicle.bbHeight * 0.6, 0.0)
            val bp = BlockPos.containing(enginePos.x, enginePos.y, enginePos.z)
            LightPositionRegistry.putSpark(bp.asLong(), maxLvl, minLvl, ttlTicks = ttl)
            engine.checkBlock(bp)
        }
    }

    /**
     * Emits smooth fire light for flying or lying detached turret wrecks.
     *
     * @param wreck the turret wreck entity
     */
    @JvmStatic
    fun handleTurretWreckLight(wreck: TurretWreckEntity) {
        val level = wreck.level()
        if (!level.isClientSide) return

        if (wreck.tickCount % 2 != 0) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (player.distanceToSqr(wreck.x, wreck.y, wreck.z) > 192.0 * 192.0) return

        val random = ThreadLocalRandom.current()
        val isChoked = random.nextFloat() < 0.10f
        val maxLvl = if (isChoked) 4 else (7 + random.nextInt(2))
        val minLvl = 3

        val pos = Vec3(wreck.x, wreck.y + wreck.bbHeight * 0.5, wreck.z)
        val bp = BlockPos.containing(pos.x, pos.y, pos.z)

        LightPositionRegistry.putSpark(bp.asLong(), maxLvl, minLvl, ttlTicks = 4)
        level.lightEngine.checkBlock(bp)
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
     * Emits a massive, high-impact explosion flash when a vehicle is destroyed.
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