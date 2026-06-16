package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.Mod.Companion.queueServerWork
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.DestroyInfo
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getYRotFromVector
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.VectorTool.combineRotationsTurret
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries

/**
 * 处理载具销毁、爆炸等方法的工具类
 */
object VehicleDestroyUtils {

    /**
     * 载具销毁逻辑：处理殉爆、炮塔残骸生成等
     */
    @JvmStatic
    fun destroy(vehicle: VehicleEntity) {
        val destroyInfo = vehicle.computed().destroyInfo

        if (vehicle.vehicleType != VehicleType.AIRPLANE && vehicle.vehicleType != VehicleType.HELICOPTER && vehicle.vehicleType != VehicleType.AIRSHIP) {
            if (destroyInfo.explodePassengers) {
                if (vehicle.crash && destroyInfo.crashPassengers) {
                    crashPassengers(vehicle)
                } else {
                    explodePassengers(vehicle)
                }
            }
            vehicleExplosion(vehicle, destroyInfo)
        }

        if (vehicle.hasTurret() && destroyInfo.sympatheticDetonation && Math.random() < destroyInfo.sympatheticDetonationChance) {
            vehicle.sympatheticDetonated = true
            val turretWreckEntity = TurretWreckEntity(ModEntities.TURRET_WRECK.get(), vehicle.level())
            if (vehicle.turretPos != null) {
                val pos = vehicle.turretPos?.let { vehicle.position().add(it) }
                pos?.let { turretWreckEntity.setPos(it.x, it.y, it.z) }
            } else {
                turretWreckEntity.setPos(vehicle.x, vehicle.eyeY, vehicle.z)
            }

            val dir = vehicle.getUpVec(1f) + (vehicle.deltaMovement + Vec3(0.0, vehicle.computed().gravity, 0.0))

            val rdm = (Math.random() - 0.5) * 0.4 + 1
            turretWreckEntity.deltaMovement = Vec3(dir.x, dir.y, dir.z).normalize().add(
                vehicle.getRandom().triangle(0.0, 0.0172275 * 12.0),
                vehicle.getRandom().triangle(0.0, 0.0172275 * 12.0),
                vehicle.getRandom().triangle(0.0, 0.0172275 * 12.0)
            ).scale(destroyInfo.sympatheticDetonationForce.toDouble() * rdm)

            val quaternion = combineRotationsTurret(1f, vehicle)
            turretWreckEntity.vehicleName = ForgeRegistries.ENTITY_TYPES.getKey(vehicle.type).toString()
            turretWreckEntity.xRot = vehicle.getTurretPitch(1f)
            turretWreckEntity.yRot = -getYRotFromVector(vehicle.getBarrelVector(1f)).toFloat()
            turretWreckEntity.setQuaternion0(quaternion)
            turretWreckEntity.setQuaternion(quaternion)
            vehicle.level().addFreshEntity(turretWreckEntity)
        }

        if (destroyInfo.noWreck) {
            vehicle.discard()
        }
    }

    /**
     * 载具爆炸
     */
    @JvmStatic
    fun vehicleExplosion(vehicle: VehicleEntity, destroyInfo: DestroyInfo) {
        val radius = destroyInfo.explosionRadius
        if (radius > 0) {
            queueServerWork(1) {
                val damage = destroyInfo.explosionDamage

                val explosion = CustomExplosion.Builder(vehicle)
                    .attacker(vehicle.lastAttacker)
                    .radius(radius)
                    .damage(damage)

                if (!destroyInfo.explodeBlocks) {
                    explosion.keepBlock()
                }

                explosion.explode()
            }
        }
    }

    /**
     * 空中坠毁时对乘员的伤害
     */
    @JvmStatic
    fun crashPassengers(vehicle: VehicleEntity) {
        for (entity in vehicle.getPassengers()) {
            if (entity is LivingEntity) {
                repeat(VehicleConfig.AIR_CRASH_EXPLOSION_COUNT.get()) {
                    val tempAttacker = if (entity === vehicle.lastAttacker) null else vehicle.lastAttacker
                    entity.invulnerableTime = 0
                    entity.hurt(
                        ModDamageTypes.causeAirCrashDamage(vehicle.level().registryAccess(), null, tempAttacker),
                        VehicleConfig.AIR_CRASH_EXPLOSION_DAMAGE.get().toFloat()
                    )
                }
            }
        }
    }

    /**
     * 载具自爆时对乘员的伤害
     */
    @JvmStatic
    fun explodePassengers(vehicle: VehicleEntity) {
        for (entity in vehicle.getPassengers()) {
            if (entity !is LivingEntity) continue
            repeat(VehicleConfig.SELF_EXPLOSION_COUNT.get()) {
                val tempAttacker = if (entity === vehicle.lastAttacker) null else vehicle.lastAttacker
                entity.invulnerableTime = 0
                entity.hurt(
                    ModDamageTypes.causeVehicleExplosionDamage(
                        vehicle.level().registryAccess(),
                        null,
                        tempAttacker
                    ), VehicleConfig.SELF_EXPLOSION_DAMAGE.get().toFloat()
                )
            }
        }
    }
}
