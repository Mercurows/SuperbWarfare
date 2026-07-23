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
import java.util.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Client-side handler for vehicle dynamic lighting events including weapon fire,
 * hull/turret burning, turret wreck illumination, and destruction explosions.
 *
 * @author paralax034
 * @since 0.8.9.1
 */
@OnlyIn(Dist.CLIENT)
object VehicleLightingHandler {

    private val random = Random()

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
            damage >= 30.0 -> {
                maxLevel = 15
                minLevel = 11
                duration = 4
            }

            damage >= 12.0 -> {
                maxLevel = 14
                minLevel = 9
                duration = 3
            }

            else -> {
                maxLevel = 13
                minLevel = 8
                duration = 2
            }
        }

        val params = MuzzleFlashHelper.FlashParams(maxLevel, minLevel, duration)
        MuzzleFlashHelper.spawnFlashCone(shootPos, shootVec, params)

        val dir = shootVec.normalize()
        val mountPos = shootPos.subtract(dir.scale(0.8))
        val mountBp = BlockPos.containing(mountPos.x, mountPos.y, mountPos.z)
        val engine = vehicle.level().lightEngine
        LightPositionRegistry.putSpark(mountBp.asLong(), (maxLevel - 2).coerceAtLeast(8), 5, duration)
        engine.checkBlock(mountBp)
    }

    /**
     * Emits dynamic ambient light for burning vehicles (turret burn, engine fire, smoldering wreck).
     *
     * @param vehicle the vehicle entity being processed
     */
    @JvmStatic
    fun handleVehicleFireLight(vehicle: VehicleEntity) {
        val level = vehicle.level()
        if (!level.isClientSide) return

        // Static emplacements (mortars, TOW, etc.) have hasLowHealthWarning = false
        // in their data — skip burning light for them to avoid strange glowing
        if (!vehicle.data().compute().hasLowHealthWarning) return

        val tick = vehicle.tickCount
        val engine = level.lightEngine
        val scale = ((vehicle.bbWidth + vehicle.bbHeight) / 2.0f).coerceIn(1.0f, 3.5f)

        // 1. Turret Burn / Sympathetic Detonation Fire (High intensity, pulses every 2 ticks)
        if (vehicle.turretBurnTimer > 0 && !vehicle.sympatheticDetonated) {
            if (tick % 2 == 0) {
                val burnPos =
                    vehicle.turretBurnEffectPos() ?: vehicle.position().add(0.0, vehicle.bbHeight.toDouble(), 0.0)
                val upOffset = vehicle.getUpVec(1f).scale(0.8)
                val lightPoint = burnPos.add(upOffset)
                val bp = BlockPos.containing(lightPoint.x, lightPoint.y, lightPoint.z)

                val maxLvl = (13 + random.nextInt(3)).coerceAtMost(15)
                val minLvl = (8 + random.nextInt(3)).coerceAtLeast(6)
                val ttl = (3 + (scale * 0.5f).toInt()).coerceIn(3, 6)

                LightPositionRegistry.putSpark(bp.asLong(), maxLvl, minLvl, ttl)
                engine.checkBlock(bp)
            }
        }

        // 2. Critical Engine / Hull Fire (Medium-high intensity, pulses every 3 ticks)
        if (vehicle.health <= 0.1f * vehicle.getMaxHealth() && !vehicle.isWreck) {
            if (tick % 3 == 0) {
                val firePos = Vec3(vehicle.x, vehicle.y + 0.85 * vehicle.bbHeight, vehicle.z)
                val bp = BlockPos.containing(firePos.x, firePos.y, firePos.z)

                val baseMax = (10 + (scale * 1.2f).toInt()).coerceIn(10, 14)
                val maxLvl = (baseMax + random.nextInt(2) - 1).coerceIn(1, 15)
                val minLvl = (maxLvl - 4).coerceAtLeast(4)

                LightPositionRegistry.putSpark(bp.asLong(), maxLvl, minLvl, 4)
                engine.checkBlock(bp)
            }
        }

        // 3. Wreckage Smoldering Glow (Low ember intensity, pulses every 4 ticks)
        if (vehicle.isWreck) {
            if (tick % 4 == 0) {
                val wreckPos = Vec3(vehicle.x, vehicle.y + 0.5 * vehicle.bbHeight, vehicle.z)
                val bp = BlockPos.containing(wreckPos.x, wreckPos.y, wreckPos.z)

                val maxLvl = (7 + random.nextInt(3)).coerceIn(6, 9)
                val minLvl = 4

                LightPositionRegistry.putSpark(bp.asLong(), maxLvl, minLvl, 5)
                engine.checkBlock(bp)
            }
        }
    }

    /**
     * Emits smooth pulsing fire light for flying or smoldering detached turret wrecks.
     * @param wreck the turret wreck entity
     */
    @JvmStatic
    fun handleTurretWreckLight(wreck: TurretWreckEntity) {
        val level = wreck.level()
        if (!level.isClientSide) return

        // Update every 3 ticks — TTL 9 = 3× interval, prevents snap-off gaps
        if (wreck.tickCount % 3 != 0) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (player.distanceToSqr(wreck.x, wreck.y, wreck.z) > 192.0 * 192.0) return

        // Sine wave period = 60 ticks (~3 seconds) → smooth breathing cycle
        val phase = (wreck.tickCount % 60).toDouble() / 60.0
        val sine = sin(phase * 2.0 * PI)
        // Base 10, amplitude ±2 → range [8, 12]
        val maxLvl = (10 + (2.0 * sine).toInt()).coerceIn(8, 12)
        val minLvl = (maxLvl - 1).coerceAtLeast(7)

        val pos = Vec3(wreck.x, wreck.y + wreck.bbHeight * 0.6, wreck.z)
        val bp = BlockPos.containing(pos.x, pos.y, pos.z)

        LightPositionRegistry.putSpark(bp.asLong(), maxLvl, minLvl, 9)
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
     * Emits an explosion flash when a vehicle is destroyed, scaled by vehicle physical size.
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
        val effectiveRadius = baseRadius * sizeFactor

        val center = Vec3(vehicle.x, vehicle.y + vehicle.bbHeight * 0.5, vehicle.z)
        ProjectileLightHelper.emitExplosionFlashDirect(level, center, effectiveRadius)
    }
}
