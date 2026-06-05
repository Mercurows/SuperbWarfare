package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.client.particle.CannonMuzzleFlareOption
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.OBBEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModParticleTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.OBB.Part.*
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.toVec3
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

/**
 * 处理载具特效、粒子、低血量警告等方法的工具类
 */
object VehicleEffectUtils {

    /**
     * 在载具位置添加随机粒子
     */
    @JvmStatic
    fun addRandomParticle(
        vehicle: VehicleEntity,
        particleOptions: ParticleOptions,
        pos: Vec3,
        randomPos: Float,
        level: Level,
        speed: Float,
        count: Int
    ) {
        val randomX = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
        val randomY = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
        val randomZ = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
        repeat(count) {
            level.addAlwaysVisibleParticle(
                particleOptions,
                true,
                pos.x + randomPos * randomX,
                pos.y + randomPos * randomY,
                pos.z + randomPos * randomZ,
                (randomX * speed).toDouble(),
                (randomY * speed).toDouble(),
                (randomZ * speed).toDouble()
            )
        }
    }

    /**
     * 在载具位置添加随机粒子（指定方向速度）
     */
    @JvmStatic
    fun addRandomParticle(
        vehicle: VehicleEntity,
        particleOptions: ParticleOptions,
        pos: Vec3,
        randomPos: Float,
        level: Level,
        count: Int,
        vec3: Vec3
    ) {
        val randomX = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
        val randomY = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
        val randomZ = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
        repeat(count) {
            level.addAlwaysVisibleParticle(
                particleOptions,
                true,
                pos.x + randomPos * randomX,
                pos.y + randomPos * randomY,
                pos.z + randomPos * randomZ,
                vec3.x,
                vec3.y,
                vec3.z
            )
        }
    }

    /**
     * 默认的部件损坏粒子效果
     */
    @JvmStatic
    fun defaultPartDamageEffect(vehicle: VehicleEntity, pos: Vec3) {
        if (vehicle.level().isClientSide) {
            addRandomParticle(vehicle, ModParticleTypes.FIRE_STAR.get(), pos, 0f, vehicle.level(), 0.25f, 1)
            addRandomParticle(vehicle, ParticleTypes.LARGE_SMOKE, pos, 0.5f, vehicle.level(), 0.001f, 1)
        }
    }

    /**
     * 处理载具部件损坏
     */
    @JvmStatic
    fun handlePartDamaged(vehicle: VehicleEntity, obbEntity: OBBEntity) {
        val obbList = obbEntity.getOBBs()
        for (obb in obbList) {
            val pos = obb.center.toVec3()
            when (obb.part) {
                TURRET -> {
                    if (vehicle.turretDamaged) {
                        vehicle.onTurretDamaged(pos)
                    }
                }

                WHEEL_LEFT -> {
                    if (vehicle.leftWheelDamaged) {
                        vehicle.onLeftWheelDamaged(pos)
                    }
                }

                WHEEL_RIGHT -> {
                    if (vehicle.rightWheelDamaged) {
                        vehicle.onRightWheelDamaged(pos)
                    }
                }

                MAIN_ENGINE -> {
                    if (vehicle.mainEngineDamaged) {
                        vehicle.onEngine1Damaged(pos)
                    }
                }

                SUB_ENGINE -> {
                    if (vehicle.subEngineDamaged) {
                        vehicle.onEngine2Damaged(pos)
                    }
                }

                else -> {}
            }
        }
    }

    /**
     * 处理载具部件血量
     */
    @JvmStatic
    fun handlePartHealth(vehicle: VehicleEntity) {
        if (vehicle.hasTurret() && (vehicle.vehicleType == VehicleType.AA || vehicle.vehicleType == VehicleType.APC || vehicle.vehicleType == VehicleType.TANK) && vehicle.health < 0.05 * vehicle.getMaxHealth()) {
            vehicle.turretHealth = 0f
            vehicle.mainEngineHealth = 0f
            vehicle.subEngineHealth = 0f
        }
        if ((vehicle.vehicleType == VehicleType.HELICOPTER || vehicle.vehicleType == VehicleType.AIRPLANE || vehicle.vehicleType == VehicleType.AIRSHIP) && vehicle.health < 0.05 * vehicle.getMaxHealth()) {
            vehicle.mainEngineHealth = 0f
            vehicle.subEngineHealth = 0f
        }

        if (vehicle.turretHealth <= 0) {
            vehicle.turretDamaged = true
        } else if (vehicle.turretHealth > 0.95 * vehicle.getTurretMaxHealth()) {
            vehicle.turretDamaged = false
        }

        if (vehicle.leftWheelHealth <= 0) {
            vehicle.leftWheelDamaged = true
        } else if (vehicle.leftWheelHealth > 0.95 * vehicle.getWheelMaxHealth()) {
            vehicle.leftWheelDamaged = false
        }

        if (vehicle.rightWheelHealth <= 0) {
            vehicle.rightWheelDamaged = true
        } else if (vehicle.rightWheelHealth > 0.95 * vehicle.getWheelMaxHealth()) {
            vehicle.rightWheelDamaged = false
        }

        if (vehicle.mainEngineHealth <= 0) {
            vehicle.mainEngineDamaged = true
        } else if (vehicle.mainEngineHealth > 0.95 * vehicle.getEngineMaxHealth()) {
            vehicle.mainEngineDamaged = false
        }

        if (vehicle.subEngineHealth <= 0) {
            vehicle.subEngineDamaged = true
        } else if (vehicle.subEngineHealth > 0.95 * vehicle.getEngineMaxHealth()) {
            vehicle.subEngineDamaged = false
        }

        if (!vehicle.isWreck) {
            vehicle.turretHealth = kotlin.math.min(
                vehicle.turretHealth + 0.0025f * vehicle.getTurretMaxHealth(),
                vehicle.getTurretMaxHealth()
            )
            vehicle.leftWheelHealth = kotlin.math.min(
                vehicle.leftWheelHealth + 0.0025f * vehicle.getWheelMaxHealth(),
                vehicle.getWheelMaxHealth()
            )
            vehicle.rightWheelHealth = kotlin.math.min(
                vehicle.rightWheelHealth + 0.0025f * vehicle.getWheelMaxHealth(),
                vehicle.getWheelMaxHealth()
            )
            vehicle.mainEngineHealth = kotlin.math.min(
                vehicle.mainEngineHealth + 0.0025f * vehicle.getEngineMaxHealth(),
                vehicle.getEngineMaxHealth()
            )
            vehicle.subEngineHealth = kotlin.math.min(
                vehicle.subEngineHealth + 0.0025f * vehicle.getEngineMaxHealth(),
                vehicle.getEngineMaxHealth()
            )
        }
    }

    /**
     * 播放低血量粒子效果
     */
    @JvmStatic
    fun playLowHealthParticle(vehicle: VehicleEntity) {
        if (vehicle.level().isClientSide) {
            addRandomParticle(
                vehicle,
                ParticleTypes.LARGE_SMOKE,
                Vec3(vehicle.x, vehicle.y + 0.7f * vehicle.bbHeight, vehicle.z),
                0.35f * vehicle.bbWidth,
                vehicle.level(),
                0.01f,
                1
            )
            addRandomParticle(
                vehicle,
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                Vec3(vehicle.x, vehicle.y + 0.7f * vehicle.bbHeight, vehicle.z),
                0.35f * vehicle.bbWidth,
                vehicle.level(),
                0.01f,
                1
            )
        }
    }

    /**
     * 获取炮塔燃烧的效果位置
     */
    @JvmStatic
    fun turretBurnEffectPos(vehicle: VehicleEntity): Vec3? {
        val pos = vehicle.turretPos ?: return null
        val worldPosition = VehicleVecUtils.transformPosition(
            vehicle.getVehicleTransform(1f),
            pos.x, pos.y, pos.z
        )
        return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
    }

    /**
     * 低血量警告：生成烟雾粒子、火焰粒子、播放音效
     */
    @JvmStatic
    fun lowHealthWarning(vehicle: VehicleEntity) {
        if (!vehicle.data().compute().hasLowHealthWarning) return
        if (vehicle.health <= 0.4 * vehicle.getMaxHealth()) {
            addRandomParticle(
                vehicle,
                ParticleTypes.LARGE_SMOKE,
                Vec3(vehicle.x, vehicle.y + 0.7f * vehicle.bbHeight, vehicle.z),
                0.35f * vehicle.bbWidth,
                vehicle.level(),
                0.01f,
                1
            )
        }

        if (vehicle.health <= 0.25 * vehicle.getMaxHealth()) {
            playLowHealthParticle(vehicle)
        }
        if (vehicle.health <= 0.15 * vehicle.getMaxHealth()) {
            playLowHealthParticle(vehicle)
        }

        if (vehicle.health <= 0.1 * vehicle.getMaxHealth()) {
            val random = 2 * (vehicle.getRandom().nextFloat() - 0.5f)
            if (vehicle.level().isClientSide) {
                addRandomParticle(
                    vehicle,
                    ParticleTypes.LARGE_SMOKE,
                    Vec3(vehicle.x, vehicle.y + 0.7f * vehicle.bbHeight, vehicle.z),
                    0.35f * vehicle.bbWidth,
                    vehicle.level(),
                    0.01f,
                    2
                )
                addRandomParticle(
                    vehicle,
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    Vec3(vehicle.x, vehicle.y + 0.7f * vehicle.bbHeight, vehicle.z),
                    0.35f * vehicle.bbWidth,
                    vehicle.level(),
                    0.01f,
                    2
                )
                addRandomParticle(
                    vehicle,
                    CustomCloudOption(
                        1f,
                        0.1f,
                        0f,
                        (240 + 40 * random).toInt(),
                        2.5f + 0.5f * random,
                        -0.07f,
                        cooldown = true,
                        light = true
                    ),
                    Vec3(vehicle.x, vehicle.y + 0.85f * vehicle.bbHeight, vehicle.z),
                    0.35f * vehicle.bbWidth,
                    vehicle.level(),
                    0.01f,
                    1
                )
                addRandomParticle(
                    vehicle,
                    CustomCloudOption(
                        1f,
                        0.35f,
                        0f,
                        (80 + 40 * random).toInt(),
                        1.5f + 0.5f * random,
                        -0.07f,
                        cooldown = false,
                        light = true
                    ),
                    Vec3(vehicle.x, vehicle.y + 0.85f * vehicle.bbHeight, vehicle.z),
                    0.3f * vehicle.bbWidth,
                    vehicle.level(),
                    0.01f,
                    1
                )
            }

            if (vehicle.computed().destroyInfo.sympatheticDetonation
                && vehicle.health < 0.05 * vehicle.getMaxHealth() && vehicle.hasTurret()
                && (vehicle.vehicleType == VehicleType.AA || vehicle.vehicleType == VehicleType.APC || vehicle.vehicleType == VehicleType.TANK)
                && !vehicle.sympatheticDetonated
                && !vehicle.turretBurned
            ) {
                vehicle.turretBurned = true
                vehicle.turretBurnTimer = 400
            }

            if (vehicle.turretBurnTimer > 0 && !vehicle.sympatheticDetonated && vehicle.health < 0.05 * vehicle.getMaxHealth()) {
                if (vehicle.level().isClientSide) {
                    val pos = turretBurnEffectPos(vehicle)
                    val dir = vehicle.getUpVec(1f)
                    ParticleTool.spawnDirectionalParticles(
                        (12 + 10 * random).toInt(),
                        0.05 * random.toDouble(),
                        vehicle.level(),
                        CannonMuzzleFlareOption(1f, 0.97f, 0.97f, 4, 0.5f, 1, 0.3f),
                        dir,
                        pos,
                        4.5 + random
                    )
                    ParticleTool.spawnDirectionalParticles(
                        (4 + 4 * random).toInt(),
                        0.8 * random.toDouble(),
                        vehicle.level(),
                        ModParticleTypes.FIRE_STAR.get(),
                        dir,
                        pos,
                        0.4 + random
                    )
                    ParticleTool.spawnDirectionalParticles(
                        (4 + 4 * random).toInt(),
                        0.8 * random.toDouble(),
                        vehicle.level(),
                        ParticleTypes.LAVA,
                        dir,
                        pos,
                        0.4 + random
                    )
                    ParticleTool.spawnDirectionalParticles(
                        (4 + 4 * random).toInt(),
                        0.8 * random.toDouble(),
                        vehicle.level(),
                        ParticleTypes.FLAME,
                        dir,
                        pos,
                        0.4 + random
                    )
                }

                if (vehicle.turretBurnTimer == 400) {
                    vehicle.level().playSound(
                        null,
                        vehicle.onPos,
                        ModSounds.TURRET_BURN_START.get(),
                        SoundSource.BLOCKS,
                        4f,
                        1f + 0.05f * random
                    )
                }
                if (vehicle.turretBurnTimer % 5 == 0) {
                    vehicle.level().playSound(
                        null,
                        vehicle.onPos,
                        ModSounds.TURRET_BURN.get(),
                        SoundSource.BLOCKS,
                        1.5f,
                        1f + 0.05f * random
                    )
                }
            }

            if (vehicle.health > 0.05 * vehicle.getMaxHealth()) {
                vehicle.turretBurned = false
                vehicle.turretBurnTimer = 0
            }

            if (vehicle.tickCount % 15 == 0) {
                vehicle.level().playSound(null, vehicle.onPos, SoundEvents.FIRE_AMBIENT, SoundSource.PLAYERS, 1f, 1f)
            }
        }

        if (vehicle.health > 0 && vehicle.health < 0.1f * vehicle.getMaxHealth() && vehicle.tickCount % 13 == 0) {
            vehicle.level().playSound(null, vehicle.onPos, ModSounds.NO_HEALTH.get(), SoundSource.PLAYERS, 1f, 1f)
        } else if (vehicle.health >= 0.1f && vehicle.health < 0.4f * vehicle.getMaxHealth() && vehicle.tickCount % 10 == 0) {
            vehicle.level().playSound(null, vehicle.onPos, ModSounds.LOW_HEALTH.get(), SoundSource.PLAYERS, 1f, 1f)
        }
    }
}
