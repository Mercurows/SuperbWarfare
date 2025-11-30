package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.*
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.VectorTool.calculateY
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.joml.Math
import kotlin.math.min

object VehicleEngineUtils {
    @JvmStatic
    fun trackEngine(vehicle: VehicleEntity, engineInfo: Track) {
        val buoyancy = engineInfo.buoyancy
        val data = vehicle.getEntityData()
        val energyCost = (engineInfo.energyCostRate * Mth.abs(data.get(VehicleEntity.POWER))).toInt()
        val wheelRotSpeed = engineInfo.wheelRotSpeed
        val wheelDifferential = engineInfo.wheelDifferential
        val trackSpeed = engineInfo.trackRotSpeed
        val trackDifferential = engineInfo.trackDifferential
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val steeringSpeed = engineInfo.steeringSpeed

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle)
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, fluidFloat, 0.0))
        }

        if (vehicle.onGround()) {
            val f0 = 0.54f + 0.25f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.05 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble()))
        } else if (vehicle.isInFluidType) {
            val f1 = 0.74f + 0.09f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.04 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble()))
        } else {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.99, 0.99, 0.99))
        }

        val passenger0 = vehicle.getFirstPassenger()

        if (vehicle.energy <= energyCost) return

        if (passenger0 == null) {
            vehicle.setLeftInputDown(false)
            vehicle.setRightInputDown(false)
            vehicle.setForwardInputDown(false)
            vehicle.setBackInputDown(false)
            data.set(VehicleEntity.POWER, 0f)
        }

        if (vehicle.forwardInputDown()) {
            data.set(
                VehicleEntity.POWER, Math.min(
                    data.get(VehicleEntity.POWER) + (if (data.get(VehicleEntity.POWER) < 0) powerAdd * 2f else powerAdd),
                    1f
                )
            )
        }

        if (vehicle.backInputDown()) {
            data.set(
                VehicleEntity.POWER, Math.max(
                    data.get(VehicleEntity.POWER) - (if (data.get(VehicleEntity.POWER) > 0) powerReduce * 2f else powerReduce),
                    -1f
                )
            )
            if (vehicle.rightInputDown()) {
                data.set(
                    VehicleEntity.DELTA_ROT, data.get(
                        VehicleEntity.DELTA_ROT
                    ) + steeringSpeed
                )
            } else if (vehicle.leftInputDown()) {
                data.set(
                    VehicleEntity.DELTA_ROT, data.get(
                        VehicleEntity.DELTA_ROT
                    ) - steeringSpeed
                )
            }
        } else {
            if (vehicle.rightInputDown()) {
                data.set(
                    VehicleEntity.DELTA_ROT, data.get(
                        VehicleEntity.DELTA_ROT
                    ) - steeringSpeed
                )
            } else if (vehicle.leftInputDown()) {
                data.set(
                    VehicleEntity.DELTA_ROT, data.get(
                        VehicleEntity.DELTA_ROT
                    ) + steeringSpeed
                )
            }
        }

        if (data.get(VehicleEntity.POWER) > 0) {
            vehicle.targetSpeed = (maxForwardSpeedRate * (1 + vehicle.xRot / 55)).toDouble()
        } else {
            vehicle.targetSpeed = (maxBackwardSpeedRate * (1 - vehicle.xRot / 55)).toDouble()
        }

        if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.96f
            )
        }

        if (vehicle.upInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.6f
            )
        }

        if (vehicle.rightInputDown() || vehicle.leftInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.96f
            )
        }

        if (vehicle.level() is ServerLevel) {
            vehicle.consumeEnergy(energyCost)
        }

        data.set(
            VehicleEntity.DELTA_ROT, data.get(
                VehicleEntity.DELTA_ROT
            ) * Math.max(0.76f - 0.1f * vehicle.deltaMovement.horizontalDistance(), 0.3).toFloat()
        )

        val s0 = vehicle.deltaMovement.dot(vehicle.getViewVector(1f))

        vehicle.leftWheelRot = ((vehicle.leftWheelRot - wheelRotSpeed * s0) + Mth.clamp(
            wheelDifferential * data.get(
                VehicleEntity.DELTA_ROT
            ), -5.0, 5.0
        )).toFloat()
        vehicle.rightWheelRot = ((vehicle.rightWheelRot - wheelRotSpeed * s0) - Mth.clamp(
            wheelDifferential * data.get(
                VehicleEntity.DELTA_ROT
            ), -5.0, 5.0
        )).toFloat()

        vehicle.leftTrack = ((vehicle.leftTrack - trackSpeed * java.lang.Math.PI * s0) + Mth.clamp(
            trackDifferential * java.lang.Math.PI * data.get(
                VehicleEntity.DELTA_ROT
            ), -5.0, 5.0
        )).toFloat()
        vehicle.rightTrack = ((vehicle.rightTrack - trackSpeed * java.lang.Math.PI * s0) - Mth.clamp(
            trackDifferential * java.lang.Math.PI * data.get(
                VehicleEntity.DELTA_ROT
            ), -5.0, 5.0
        )).toFloat()

        val i: Int
        if (data.get(VehicleEntity.L_WHEEL_DAMAGED) && data
                .get<Boolean>(
                    VehicleEntity.R_WHEEL_DAMAGED
                )
        ) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.93f
            )
            i = 0
        } else if (data.get(VehicleEntity.L_WHEEL_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.975f
            )
            i = 3
        } else if (data.get(VehicleEntity.R_WHEEL_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.975f
            )
            i = -3
        } else {
            i = 0
        }

        if (data.get(VehicleEntity.MAIN_ENGINE_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.96f
            )
        }

        vehicle.yRot =
            (vehicle.yRot - (if (vehicle.isInFluidType && !vehicle.onGround()) 2.5 else 6.0) * data
                .get<Float>(
                    VehicleEntity.DELTA_ROT
                ) - i * s0).toFloat()
        if (vehicle.isInFluidType || vehicle.onGround()) {
            val water =
                (if (!vehicle.isInFluidType && !vehicle.onGround()) 0.05f else (if (vehicle.isInFluidType && !vehicle.onGround()) 0.3f else 1f)).toDouble()
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).scale(
                        0.15 * water * vehicle.targetSpeed * data.get(
                            VehicleEntity.POWER
                        )
                    )
                )
            )
        }
    }

    @JvmStatic
    fun wheelEngine(vehicle: VehicleEntity, engineInfo: Wheel) {
        val buoyancy = engineInfo.buoyancy
        val data = vehicle.getEntityData()
        val energyCost = (engineInfo.energyCostRate * Mth.abs(data.get(VehicleEntity.POWER))).toInt()
        val wheelRotSpeed = engineInfo.wheelRotSpeed
        val wheelDifferential = engineInfo.wheelDifferential
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val steeringSpeed = engineInfo.steeringSpeed

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle)
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, fluidFloat, 0.0))
        }

        if (vehicle.onGround()) {
            val f0 = 0.54f + 0.25f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.05 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble()))
        } else if (vehicle.isInFluidType) {
            val f1 = 0.74f + 0.09f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.04 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble()))
        } else {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.99, 0.99, 0.99))
        }

        val level = vehicle.level()
        if (level is ServerLevel && vehicle.isInFluidType && vehicle.deltaMovement.length() > 0.1) {
            ParticleTool.sendParticle(
                level,
                ParticleTypes.CLOUD,
                vehicle.x + 0.5 * vehicle.deltaMovement.x,
                vehicle.y + VehicleVecUtils.getSubmergedHeight(vehicle) - 0.2,
                vehicle.z + 0.5 * vehicle.deltaMovement.z,
                (2 + 4 * vehicle.deltaMovement.length()).toInt(),
                0.65,
                0.0,
                0.65,
                0.0,
                true
            )
            ParticleTool.sendParticle(
                level,
                ParticleTypes.BUBBLE_COLUMN_UP,
                vehicle.x + 0.5 * vehicle.deltaMovement.x,
                vehicle.y + VehicleVecUtils.getSubmergedHeight(vehicle) - 0.2,
                vehicle.z + 0.5 * vehicle.deltaMovement.z,
                (2 + 10 * vehicle.deltaMovement.length()).toInt(),
                0.65,
                0.0,
                0.65,
                0.0,
                true
            )
        }

        val passenger0 = vehicle.getFirstPassenger()

        if (vehicle.energy <= energyCost) return

        if (passenger0 == null) {
            vehicle.setLeftInputDown(false)
            vehicle.setRightInputDown(false)
            vehicle.setForwardInputDown(false)
            vehicle.setBackInputDown(false)
            data.set(VehicleEntity.POWER, 0f)
        }

        if (vehicle.forwardInputDown()) {
            data.set(
                VehicleEntity.POWER, Math.min(
                    data.get(
                        VehicleEntity.POWER
                    ) + (if (data
                            .get<Float>(VehicleEntity.POWER) < 0
                    ) powerAdd * 2f else powerAdd), 1f
                )
            )
        }

        if (vehicle.backInputDown()) {
            data.set(
                VehicleEntity.POWER, Math.max(
                    data.get(
                        VehicleEntity.POWER
                    ) - (if (data
                            .get<Float>(VehicleEntity.POWER) > 0
                    ) powerReduce * 2f else powerReduce), -1f
                )
            )
        }

        if (data.get(VehicleEntity.POWER) > 0) {
            vehicle.targetSpeed = maxForwardSpeedRate.toDouble() * (1 + vehicle.xRot / 55)
        } else {
            vehicle.targetSpeed = maxBackwardSpeedRate.toDouble() * (1 - vehicle.xRot / 55)
        }

        if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.97f
            )
        }

        if (vehicle.upInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.6f
            )
        }

        if (vehicle.rightInputDown() || vehicle.leftInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.98f
            )
        }

        if (level is ServerLevel) {
            vehicle.consumeEnergy(energyCost)
        }

        val i: Int
        if (data.get(VehicleEntity.L_WHEEL_DAMAGED) && data
                .get<Boolean>(
                    VehicleEntity.R_WHEEL_DAMAGED
                )
        ) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.93f
            )
            i = 0
        } else if (data.get(VehicleEntity.L_WHEEL_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.975f
            )
            i = 3
        } else if (data.get(VehicleEntity.R_WHEEL_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.975f
            )
            i = -3
        } else {
            i = 0
        }

        if (data.get(VehicleEntity.MAIN_ENGINE_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.875f
            )
        }

        if (vehicle.rightInputDown()) {
            data.set(
                VehicleEntity.DELTA_ROT, data.get(
                    VehicleEntity.DELTA_ROT
                ) + steeringSpeed
            )
        } else if (vehicle.leftInputDown()) {
            data.set(
                VehicleEntity.DELTA_ROT, data.get(
                    VehicleEntity.DELTA_ROT
                ) - steeringSpeed
            )
        }

        data.set(
            VehicleEntity.DELTA_ROT, data.get(
                VehicleEntity.DELTA_ROT
            ) * Math.max(0.78f - 0.25f * vehicle.deltaMovement.horizontalDistance(), 0.1).toFloat()
        )

        val s0 = vehicle.deltaMovement.dot(vehicle.getViewVector(1f))

        vehicle.leftWheelRot = ((vehicle.leftWheelRot - wheelRotSpeed * s0) - Mth.clamp(
            wheelDifferential * data.get(
                VehicleEntity.DELTA_ROT
            ), -5.0, 5.0
        ) * vehicle.deltaMovement.length()).toFloat()
        vehicle.rightWheelRot = ((vehicle.rightWheelRot - wheelRotSpeed * s0) + Mth.clamp(
            wheelDifferential * data.get(
                VehicleEntity.DELTA_ROT
            ), -5.0, 5.0
        ) * vehicle.deltaMovement.length()).toFloat()

        vehicle.rudderRot = Mth.clamp(
            vehicle.rudderRot - data.get(VehicleEntity.DELTA_ROT),
            -0.8f,
            0.8f
        ) * 0.75f

        vehicle.yRot = (vehicle.yRot - Math.max(
            (if (vehicle.isInFluidType && !vehicle.onGround()) 6 else 12) * vehicle.deltaMovement
                .horizontalDistance(), 0.0
        ) * vehicle.rudderRot * (if (data.get(
                VehicleEntity.POWER
            ) > 0
        ) 1 else -1) - i * s0).toFloat()

        if (vehicle.isInFluidType || vehicle.onGround()) {
            val water =
                (if (!vehicle.isInFluidType && !vehicle.onGround()) 0.05f else (if (vehicle.isInFluidType && !vehicle.onGround()) 0.3f else 1f)).toDouble()
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).scale(
                        0.15 * water * vehicle.targetSpeed * data.get(
                            VehicleEntity.POWER
                        )
                    )
                )
            )
        }
    }

    @JvmStatic
    fun shipEngine(vehicle: VehicleEntity, engineInfo: Ship) {
        val buoyancy = engineInfo.buoyancy
        val energyCost = (engineInfo.energyCostRate * Mth.abs(
            vehicle.getEntityData().get(VehicleEntity.POWER)
        )).toInt()
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val steeringSpeed = engineInfo.steeringSpeed
        val bodyPitchRate = engineInfo.bodyPitchRate
        val bodyRollRate = engineInfo.bodyRollRate

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle)
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, fluidFloat, 0.0))
        }

        if (vehicle.onGround()) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.2, 0.99, 0.2))
        } else if (vehicle.isInFluidType) {
            val f = (0.75f - (0.04f * min(
                VehicleVecUtils.getSubmergedHeight(vehicle),
                vehicle.bbHeight.toDouble()
            )) + 0.09f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90).toFloat()
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.04 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f.toDouble(), 0.85, f.toDouble()))
        } else {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.99, 0.99, 0.99))
        }

        val level = vehicle.level()
        if (level is ServerLevel && vehicle.isInFluidType && vehicle.deltaMovement.length() > 0.1) {
            val y = vehicle.y + VehicleVecUtils.getSubmergedHeight(vehicle) - 0.2
            ParticleTool.sendParticle(
                level,
                ParticleTypes.CLOUD,
                vehicle.x + 0.5 * vehicle.deltaMovement.x,
                y,
                vehicle.z + 0.5 * vehicle.deltaMovement.z,
                (2 + 4 * vehicle.deltaMovement.length()).toInt(),
                0.65,
                0.0,
                0.65,
                0.0,
                true
            )
            ParticleTool.sendParticle(
                level,
                ParticleTypes.BUBBLE_COLUMN_UP,
                vehicle.x + 0.5 * vehicle.deltaMovement.x,
                y,
                vehicle.z + 0.5 * vehicle.deltaMovement.z,
                (2 + 10 * vehicle.deltaMovement.length()).toInt(),
                0.65,
                0.0,
                0.65,
                0.0,
                true
            )
            ParticleTool.sendParticle(
                level,
                ParticleTypes.BUBBLE_COLUMN_UP,
                vehicle.x - 4.5 * vehicle.lookAngle.x,
                vehicle.y - 0.25,
                vehicle.z - 4.5 * vehicle.lookAngle.z,
                (40 * Mth.abs(
                    vehicle.getEntityData().get(VehicleEntity.POWER)
                )).toInt(),
                0.15,
                0.15,
                0.15,
                0.02,
                true
            )
        }

        val passenger0 = vehicle.getFirstPassenger()

        if (vehicle.energy > energyCost) {
            if (passenger0 == null) {
                vehicle.setLeftInputDown(false)
                vehicle.setRightInputDown(false)
                vehicle.setForwardInputDown(false)
                vehicle.setBackInputDown(false)
            }

            if (vehicle.forwardInputDown()) {
                vehicle.getEntityData().set(
                    VehicleEntity.POWER, Math.min(
                        vehicle.getEntityData().get(VehicleEntity.POWER) + (if (vehicle.getEntityData()
                                .get(VehicleEntity.POWER) < 0
                        ) powerAdd * 2f else powerAdd), 1f
                    )
                )
            }

            if (vehicle.backInputDown()) {
                vehicle.getEntityData().set(
                    VehicleEntity.POWER, Math.max(
                        vehicle.getEntityData().get(
                            VehicleEntity.POWER
                        ) - (if (vehicle.getEntityData()
                                .get(VehicleEntity.POWER) > 0
                        ) powerReduce * 2f else powerReduce), -1f
                    )
                )
            }

            if (vehicle.getEntityData().get(VehicleEntity.POWER) > 0) {
                vehicle.targetSpeed = maxForwardSpeedRate.toDouble()
            } else {
                vehicle.targetSpeed = maxBackwardSpeedRate.toDouble()
            }

            if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
                vehicle.getEntityData().set(
                    VehicleEntity.POWER, vehicle.getEntityData().get(
                        VehicleEntity.POWER
                    ) * 0.97f
                )
            }

            if (vehicle.rightInputDown() || vehicle.leftInputDown()) {
                vehicle.getEntityData().set(
                    VehicleEntity.POWER, vehicle.getEntityData().get(
                        VehicleEntity.POWER
                    ) * 0.98f
                )
            }

            if (vehicle.getEntityData().get(VehicleEntity.MAIN_ENGINE_DAMAGED)) {
                vehicle.getEntityData().set(
                    VehicleEntity.POWER, vehicle.getEntityData().get<Float>(
                        VehicleEntity.POWER
                    ) * 0.875f
                )
            }

            if (level is ServerLevel) {
                vehicle.consumeEnergy(energyCost)
            }

            if (vehicle.rightInputDown()) {
                vehicle.getEntityData().set(
                    VehicleEntity.DELTA_ROT, vehicle.getEntityData().get<Float>(
                        VehicleEntity.DELTA_ROT
                    ) - steeringSpeed
                )
            } else if (vehicle.leftInputDown()) {
                vehicle.getEntityData().set(
                    VehicleEntity.DELTA_ROT, vehicle.getEntityData().get<Float>(
                        VehicleEntity.DELTA_ROT
                    ) + steeringSpeed
                )
            }

            vehicle.getEntityData().set<Float>(
                VehicleEntity.DELTA_ROT, vehicle.getEntityData().get<Float>(
                    VehicleEntity.DELTA_ROT
                ) * Math.max(0.78f - 0.25f * vehicle.deltaMovement.horizontalDistance(), 0.1).toFloat()
            )

            vehicle.propellerRot += 2 * vehicle.getEntityData().get<Float>(VehicleEntity.POWER)
            vehicle.rudderRot = Mth.clamp(
                vehicle.rudderRot - vehicle.getEntityData().get<Float>(VehicleEntity.DELTA_ROT),
                -0.8f,
                0.8f
            ) * 0.75f

            if (vehicle.isInFluidType || vehicle.isUnderWater) {
                vehicle.xRot *= 0.85f
                val direct = (90 - VehicleVecUtils.calculateAngle(vehicle.deltaMovement, vehicle.getViewVector(1f))
                    .toFloat()) / 90
                vehicle.xRot =
                    (vehicle.xRot - direct * (if (vehicle.onGround()) 0 else 1) * bodyPitchRate * vehicle.deltaMovement
                        .horizontalDistance()).toFloat()
                vehicle.yRot = (vehicle.yRot - 20 * vehicle.deltaMovement.horizontalDistance() * vehicle.getEntityData()
                    .get<Float>(
                        VehicleEntity.DELTA_ROT
                    ) * (if (vehicle.getEntityData()
                        .get<Float>(VehicleEntity.POWER) > 0
                ) 1 else -1)).toFloat()
                vehicle.setZRot(
                    (vehicle.roll - direct * vehicle.getEntityData()
                        .get<Float>(VehicleEntity.DELTA_ROT) * (if (vehicle.onGround()) 0 else 1) * bodyRollRate * 10 * vehicle.deltaMovement
                        .horizontalDistance()).toFloat()
                )
                vehicle.setDeltaMovement(
                    vehicle.deltaMovement.add(
                        vehicle.getViewVector(1f).scale(
                            0.15 * vehicle.targetSpeed * vehicle.getEntityData().get<Float>(
                                VehicleEntity.POWER
                            )
                        )
                    )
                )
            } else {
                vehicle.xRot *= 0.99f
            }
        }

        vehicle.setZRot(vehicle.roll * 0.85f)
    }

    @JvmStatic
    fun helicopterEngine(vehicle: VehicleEntity, engineInfo: Helicopter) {
        val energyCost = engineInfo.energyCostRate.toInt()
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val pitchSpeed = engineInfo.pitchSpeed
        val yawSpeed = engineInfo.yawSpeed
        val rollSpeed = engineInfo.rollSpeed
        val lift = engineInfo.liftSpeed

        if (vehicle.onGround()) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.8, 1.0, 0.8))
        } else {
            vehicle.setZRot(vehicle.roll * (if (vehicle.backInputDown()) 0.9f else 0.99f))
            val f = Mth.clamp(
                0.95f - 0.015 * vehicle.deltaMovement.length() + 0.02f * Mth.abs(
                    90 - VehicleVecUtils.calculateAngle(
                        vehicle.deltaMovement,
                        vehicle.getViewVector(1f)
                    ).toFloat()
                ) / 90, 0.01, 0.99
            ).toFloat()
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).scale(
                        (if (vehicle.xRot < 0) -0.035 else (if (vehicle.xRot > 0) 0.035 else 0.0)) * vehicle.deltaMovement
                            .length()
                    )
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f.toDouble(), 0.95, f.toDouble()))
        }

        if (vehicle.isInFluidType && vehicle.tickCount % 4 == 0 && VehicleVecUtils.getSubmergedHeight(vehicle) > 0.5 * vehicle.bbHeight) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.6, 0.6, 0.6))
            vehicle.hurt(
                ModDamageTypes.causeVehicleStrikeDamage(
                    vehicle.level().registryAccess(),
                    vehicle,
                    if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                ), 6 + (20 * ((vehicle.lastTickSpeed - 0.4) * (vehicle.lastTickSpeed - 0.4))).toFloat()
            )
        }

        val pilot = vehicle.getFirstPassenger()

        var hasPassenger = false

        for (i in 0..<vehicle.maxPassengers - 1) {
            if (vehicle.getNthEntity(i) != null) {
                hasPassenger = true
            }
        }

        val diffX: Float
        val diffZ: Float

        val data = vehicle.getEntityData()
        if (vehicle.health > 0.1f * vehicle.getMaxHealth()) {
            val landingPos = findNearestLandingPos(vehicle, 30)
            if (pilot == null) {
                vehicle.setLeftInputDown(false)
                vehicle.setRightInputDown(false)
                vehicle.setForwardInputDown(false)
                vehicle.setBackInputDown(false)
                vehicle.setUpInputDown(false)
                vehicle.setDownInputDown(false)
                vehicle.setZRot(vehicle.roll * 0.98f)
                vehicle.xRot *= 0.98f
                vehicle.deltaMovement.multiply(0.96, 0.98, 0.96)
                if (hasPassenger) {
                    data.set(VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.99f)
                }
            } else {
                if (!vehicle.backInputDown() || landingPos == null) {
                    if (vehicle.rightInputDown()) {
                        vehicle.holdTick++
                        data.set(
                            VehicleEntity.DELTA_ROT,
                            data.get(VehicleEntity.DELTA_ROT) - 2f * Math.min(vehicle.holdTick, 7) * data.get(
                                VehicleEntity.POWER
                            )
                        )
                    } else if (vehicle.leftInputDown()) {
                        vehicle.holdTick++
                        data.set(
                            VehicleEntity.DELTA_ROT,
                            data.get(VehicleEntity.DELTA_ROT) + 2f * Math.min(vehicle.holdTick, 7) * data.get(
                                VehicleEntity.POWER
                            )
                        )
                    } else {
                        vehicle.holdTick = 0
                    }
                    vehicle.xRot += (if (vehicle.onGround()) 0f else 1.5f) * pitchSpeed * vehicle.mouseMoveSpeedY * data
                        .get(VehicleEntity.PROPELLER_ROT)
                    vehicle.setZRot(
                        vehicle.roll - rollSpeed * (data
                            .get(VehicleEntity.DELTA_ROT) + (if (vehicle.onGround()) 0f else 0.25f) * vehicle.mouseMoveSpeedX * data
                            .get(VehicleEntity.PROPELLER_ROT))
                    )
                }

                vehicle.yRot += yawSpeed * Mth.clamp(
                    (if (vehicle.onGround()) 0.1f else 2f) * vehicle.mouseMoveSpeedX * data
                        .get(VehicleEntity.PROPELLER_ROT) + (if (data
                            .get(VehicleEntity.SUB_ENGINE_DAMAGED)
                    ) 25 else 0) * data.get(VehicleEntity.PROPELLER_ROT), -10f, 10f
                )
                if (landingPos != null && !vehicle.onGround() && vehicle.backInputDown()) {
                    updateAutoLanding(vehicle, landingPos)
                }

                if (pilot is Player && vehicle.level().isClientSide && landingPos != null && !vehicle.onGround()) {
                    pilot.displayClientMessage(Component.translatable("tips.superbwarfare.press_s_to_landing"), true)
                }

                if (vehicle.onGround()) {
                    vehicle.setZRot(vehicle.roll * 0.98f)
                    vehicle.xRot *= 0.98f
                }
            }

            if (vehicle.energy > energyCost) {
                val up = vehicle.upInputDown() || vehicle.forwardInputDown()
                val down = vehicle.downInputDown()

                if (!vehicle.engineStart && up) {
                    vehicle.engineStart = true
                    vehicle.level()
                        .playSound(null, vehicle, engineInfo.engineStartSound, vehicle.soundSource, 3f, 1f)
                }

                if (up && vehicle.engineStartOver) {
                    vehicle.holdPowerTick++
                    data.set(
                        VehicleEntity.POWER, Math.min(
                            data.get(
                                VehicleEntity.POWER
                            ) + 0.0007f * powerAdd * Math.min(vehicle.holdPowerTick, 10), 0.12f
                        )
                    )
                }

                if (vehicle.engineStartOver) {
                    if (down) {
                        vehicle.holdPowerTick++
                        data.set(
                            VehicleEntity.POWER, Math.max(
                                data.get(
                                    VehicleEntity.POWER
                                ) - 0.001f * powerReduce * Math.min(vehicle.holdPowerTick, 5),
                                if (vehicle.onGround()) 0f else 0.025f / lift
                            )
                        )
                    } else if (vehicle.backInputDown()) {
                        vehicle.holdPowerTick++
                        data.set(
                            VehicleEntity.POWER, Math.max(
                                data.get(
                                    VehicleEntity.POWER
                                ) - 0.001f * powerReduce * Math.min(vehicle.holdPowerTick, 5),
                                if (vehicle.onGround()) 0f else 0.058f / lift
                            )
                        )
                    }
                }

                if (vehicle.engineStart && !vehicle.engineStartOver) {
                    data.set(
                        VehicleEntity.POWER, Math.min(
                            data.get(
                                VehicleEntity.POWER
                            ) + 0.0012f * powerAdd, 0.045f
                        )
                    )
                }

                if (!(up || down || vehicle.backInputDown()) && vehicle.engineStartOver) {
                    if (vehicle.deltaMovement.y() < 0) {
                        data.set(
                            VehicleEntity.POWER, Math.min(
                                data.get(
                                    VehicleEntity.POWER
                                ) + 0.0002f, 0.12f
                            )
                        )
                    } else {
                        data.set(
                            VehicleEntity.POWER, Math.max(
                                data.get(
                                    VehicleEntity.POWER
                                ) - (if (vehicle.onGround()) 0.00005f else 0.0002f), 0f
                            )
                        )
                    }
                    vehicle.holdPowerTick = 0
                }
            } else {
                data.set(
                    VehicleEntity.POWER, Math.max(
                        data.get(
                            VehicleEntity.POWER
                        ) - 0.0001f, 0f
                    )
                )
                vehicle.setForwardInputDown(false)
                vehicle.setBackInputDown(false)
                vehicle.engineStart = false
                vehicle.engineStartOver = false
            }
        } else if (!vehicle.onGround() && vehicle.engineStartOver) {
            data.set(
                VehicleEntity.POWER, Math.max(
                    data.get(
                        VehicleEntity.POWER
                    ) - 0.0003f, 0.01f
                )
            )
            vehicle.destroyRot += 0.08f

            diffX = 45 - vehicle.xRot
            diffZ = -20 - vehicle.roll

            vehicle.xRot += diffX * 0.05f * data.get(VehicleEntity.PROPELLER_ROT)
            vehicle.yRot += vehicle.destroyRot
            vehicle.setZRot(
                vehicle.roll + diffZ * 0.1f * data.get(VehicleEntity.PROPELLER_ROT)
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, -vehicle.destroyRot * 0.004, 0.0))
        }

        if (data.get(VehicleEntity.MAIN_ENGINE_DAMAGED)) {
            data.set(
                VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.98f
            )
        }

        data.set(
            VehicleEntity.DELTA_ROT, data.get(VehicleEntity.DELTA_ROT) * 0.9f
        )
        data.set(
            VehicleEntity.PROPELLER_ROT, Mth.lerp(
                0.18f, data.get(VehicleEntity.PROPELLER_ROT), data.get(VehicleEntity.POWER)
            )
        )
        vehicle.propellerRot += 30 * data.get(VehicleEntity.PROPELLER_ROT)
        data.set(
            VehicleEntity.PROPELLER_ROT, data.get(VehicleEntity.PROPELLER_ROT) * 0.9995f
        )

        if (vehicle.engineStart) {
            vehicle.consumeEnergy(
                (energyCost * 8.3333f * Mth.abs(data.get(VehicleEntity.POWER))).toInt()
            )
        }

        val force = vehicle.getUpVec(1f)

        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                force.scale(
                    (data.get(VehicleEntity.PROPELLER_ROT) * lift).toDouble()
                )
            )
        )

        if (data.get(VehicleEntity.POWER) > 0.04f) {
            vehicle.engineStartOver = true
        }

        if (data.get(VehicleEntity.POWER) < 0.0004f) {
            vehicle.engineStart = false
            vehicle.engineStartOver = false
        }
    }

    @JvmStatic
    fun aircraftEngine(vehicle: VehicleEntity, engineInfo: Aircraft) {
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val pitchSpeed = engineInfo.pitchSpeed
        val yawSpeed = engineInfo.yawSpeed
        val rollSpeed = engineInfo.rollSpeed
        val lift = engineInfo.liftSpeed
        val speedRate = engineInfo.speedRate
        val gearRotateAngle = engineInfo.gearRotateAngle
        val data = vehicle.getEntityData()
        val energyCost = (engineInfo.energyCostRate * Mth.abs(data.get(VehicleEntity.POWER))).toInt()

        val f = Mth.clamp(
            Math.max(
                (if (vehicle.onGround()) 0.819f else 0.82f) - 0.005 * vehicle.deltaMovement.length(), 0.5
            ) + 0.001f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90, 0.01, 0.99
        ).toFloat()

        val forward = vehicle.deltaMovement.dot(vehicle.getViewVector(1f)) > 0
        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                vehicle.getViewVector(1f)
                    .scale((if (forward) 0.227 else 0.1) * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
            )
        )
        vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f.toDouble(), f.toDouble(), f.toDouble()))

        if (vehicle.isInFluidType && vehicle.tickCount % 4 == 0) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.6, 0.6, 0.6))
            if (vehicle.lastTickSpeed > 0.4) {
                vehicle.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        vehicle,
                        if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                    ), (20 * ((vehicle.lastTickSpeed - 0.4) * (vehicle.lastTickSpeed - 0.4))).toFloat()
                )
            }
        }

        val passenger = vehicle.getFirstPassenger()

        if (vehicle.health > 0.1f * vehicle.getMaxHealth()) {
            if (passenger == null || vehicle.isInFluidType) {
                vehicle.setLeftInputDown(false)
                vehicle.setRightInputDown(false)
                vehicle.setForwardInputDown(false)
                vehicle.setBackInputDown(false)
                data.set(
                    VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.95f
                )
                if (vehicle.onGround()) {
                    vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.94, 1.0, 0.94))
                } else {
                    vehicle.xRot = Mth.clamp(vehicle.xRot + 0.1f, -89f, 89f)
                }
            } else if (passenger is Player) {
                if (vehicle.energy > energyCost) {
                    if (!vehicle.engineStart && vehicle.forwardInputDown()) {
                        vehicle.engineStart = true
                        if (data.get(VehicleEntity.POWER) > 0) {
                            vehicle.level()
                                .playSound(null, vehicle, engineInfo.engineStartSound, vehicle.soundSource, 3f, 1f)
                        }
                    }

                    if (vehicle.forwardInputDown()) {
                        data.set(
                            VehicleEntity.POWER, Mth.clamp(
                                (data.get(
                                    VehicleEntity.POWER
                                ) + 0.0045f * powerAdd).toDouble(), -0.1, 1.0
                            ).toFloat()
                        )
                    }

                    if (vehicle.backInputDown()) {
                        data.set(
                            VehicleEntity.POWER, Math.max(
                                data.get(
                                    VehicleEntity.POWER
                                ) - 0.006f * powerReduce, if (vehicle.onGround()) -0.2f else 0.4f
                            )
                        )
                    }
                }

                if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
                    data.set(
                        VehicleEntity.POWER, data.get(
                            VehicleEntity.POWER
                        ) * 0.995f
                    )
                }

                if (!vehicle.onGround()) {
                    if (vehicle.rightInputDown()) {
                        data.set(
                            VehicleEntity.DELTA_ROT, data.get(
                                VehicleEntity.DELTA_ROT
                            ) - 0.6f
                        )
                    } else if (vehicle.leftInputDown()) {
                        data.set(
                            VehicleEntity.DELTA_ROT, data.get(
                                VehicleEntity.DELTA_ROT
                            ) + 0.6f
                        )
                    }
                }

                // 刹车
                if (vehicle.downInputDown()) {
                    if (vehicle.onGround()) {
                        data.set(
                            VehicleEntity.POWER, data.get(
                                VehicleEntity.POWER
                            ) * 0.92f
                        )
                        vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.97, 1.0, 0.97))
                    } else {
                        data.set(
                            VehicleEntity.POWER, data.get(
                                VehicleEntity.POWER
                            ) * 0.97f
                        )
                        vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.994, 1.0, 0.994))
                    }
                    data.set(
                        VehicleEntity.PLANE_BREAK, Math.min(
                            data.get(
                                VehicleEntity.PLANE_BREAK
                            ) + 10, 60f
                        )
                    )
                }
            }

            if (vehicle.engineStart) {
                vehicle.consumeEnergy(energyCost)
            }

            val rotSpeed = 1.5f + 1.2f * Mth.abs(calculateY(vehicle.roll))

            val addY = Mth.clamp(
                Math.max(
                    (if (vehicle.onGround()) 0.6f else 0.2f) * vehicle.deltaMovement.length().toFloat(), 0f
                ) * vehicle.mouseMoveSpeedX, -rotSpeed, rotSpeed
            )
            val addX = Mth.clamp(
                Math.min(
                    Math.max(vehicle.deltaMovement.dot(vehicle.getViewVector(1f)) - 0.24, 0.15).toFloat(), 0.4f
                ) * vehicle.mouseMoveSpeedY, -3.5f, 3.5f
            )
            val addZ = data
                .get<Float>(VehicleEntity.DELTA_ROT) - (if (vehicle.onGround()) 0f else 0.004f) * vehicle.mouseMoveSpeedX * vehicle.deltaMovement
                .dot(vehicle.getViewVector(1f)).toFloat()

            vehicle.yRot = vehicle.yRot + yawSpeed * addY
            if (!vehicle.onGround()) {
                vehicle.xRot = vehicle.xRot + pitchSpeed * addX
                vehicle.setZRot(vehicle.roll - rollSpeed * addZ)
            }

            // 自动回正
            if (!vehicle.onGround()) {
                val xSpeed = 1 + 20 * Mth.abs(vehicle.xRot / 180)
                val speed = Mth.clamp(Mth.abs(vehicle.roll) / (90 / xSpeed), 0f, 1f)

                if (vehicle.roll > 0) {
                    vehicle.setZRot(vehicle.roll - Math.min(speed, vehicle.roll))
                } else if (vehicle.roll < 0) {
                    vehicle.setZRot(vehicle.roll + Math.min(speed, -vehicle.roll))
                }
            }

            vehicle.propellerRot += 30 * data.get(VehicleEntity.POWER)

            // 起落架
            if (engineInfo.hasGear) {
                if (vehicle.upInputDown()) {
                    vehicle.setUpInputDown(false)
                    if (data.get(VehicleEntity.GEAR_ROT) == 0f && !vehicle.onGround()) {
                        data.set(VehicleEntity.GEAR_UP, true)
                    } else if (data.get(VehicleEntity.GEAR_ROT) == 1f) {
                        data.set(VehicleEntity.GEAR_UP, false)
                    }
                }

                if (vehicle.onGround()) {
                    data.set(VehicleEntity.GEAR_UP, false)
                }

                if (data.get(VehicleEntity.GEAR_UP)) {
                    data.set(
                        VehicleEntity.GEAR_ROT, Math.min(
                            data.get(
                                VehicleEntity.GEAR_ROT
                            ) + 0.05f, 1f
                        )
                    )
                } else {
                    data.set(
                        VehicleEntity.GEAR_ROT, Math.max(
                            data.get(VehicleEntity.GEAR_ROT) - 0.05f, 0f
                        )
                    )
                }

                vehicle.gearRot = data.get(VehicleEntity.GEAR_ROT) * gearRotateAngle
            }

            val flapX =
                (1 - (Mth.abs(vehicle.roll)) / 90) * Mth.clamp(vehicle.mouseMoveSpeedY, -22.5f, 22.5f) - calculateY(
                    vehicle.roll
                ) * Mth.clamp(vehicle.mouseMoveSpeedX, -22.5f, 22.5f)

            vehicle.flap1LRot = Mth.clamp(
                -flapX - 4 * addZ - data.get(VehicleEntity.PLANE_BREAK),
                -22.5f,
                22.5f
            )
            vehicle.flap1RRot = Mth.clamp(
                -flapX + 4 * addZ - data.get(VehicleEntity.PLANE_BREAK),
                -22.5f,
                22.5f
            )
            vehicle.flap1L2Rot = Mth.clamp(
                -flapX - 4 * addZ + data.get(VehicleEntity.PLANE_BREAK),
                -22.5f,
                22.5f
            )
            vehicle.flap1R2Rot = Mth.clamp(
                -flapX + 4 * addZ + data.get(VehicleEntity.PLANE_BREAK),
                -22.5f,
                22.5f
            )

            vehicle.flap2LRot = Mth.clamp(flapX - 4 * addZ, -22.5f, 22.5f)
            vehicle.flap2RRot = Mth.clamp(flapX + 4 * addZ, -22.5f, 22.5f)

            val flapY =
                (1 - (Mth.abs(vehicle.roll)) / 90) * Mth.clamp(vehicle.mouseMoveSpeedX, -22.5f, 22.5f) + calculateY(
                    vehicle.roll
                ) * Mth.clamp(vehicle.mouseMoveSpeedY, -22.5f, 22.5f)
            vehicle.flap3Rot = flapY * 5
        } else if (!vehicle.onGround()) {
            val diffX: Float
            data.set(
                VehicleEntity.POWER, Math.max(
                    data.get(
                        VehicleEntity.POWER
                    ) - 0.0003f, 0.02f
                )
            )
            vehicle.destroyRot += 0.1f
            diffX = 90 - vehicle.xRot
            vehicle.xRot += diffX * 0.001f * vehicle.destroyRot
            vehicle.setZRot(vehicle.roll - vehicle.destroyRot)
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, -0.03, 0.0))
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, -vehicle.destroyRot * 0.005, 0.0))
        }

        data.set(
            VehicleEntity.DELTA_ROT, data.get(VehicleEntity.DELTA_ROT) * 0.85f
        )
        data.set(
            VehicleEntity.PLANE_BREAK, data.get(VehicleEntity.PLANE_BREAK) * 0.8f
        )
        if (vehicle.onGround()) {
            data.set(
                VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.995f
            )
        }

        if (data.get(VehicleEntity.MAIN_ENGINE_DAMAGED)) {
            data.set(VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.96f)
        }

        if (data.get(VehicleEntity.SUB_ENGINE_DAMAGED)) {
            data.set(VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.96f)
        }

        val flapAngle =
            ((vehicle.flap1LRot + vehicle.flap1RRot + vehicle.flap1L2Rot + vehicle.flap1R2Rot) / 4).toDouble()
        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                vehicle.getUpVec(1f).scale(
                    vehicle.deltaMovement
                        .dot(vehicle.getViewVector(1f)) * 0.022 * lift * (1 + Math.sin((if (vehicle.onGround()) 25.0 else flapAngle + 25) * Mth.DEG_TO_RAD))
                )
            )
        )
        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                vehicle.getViewVector(1f).scale(
                    0.03 * speedRate * data.get(VehicleEntity.POWER) * (if (vehicle.sprintInputDown()) 2.2 else 1.0)
                )
            )
        )

        if (data.get(VehicleEntity.POWER) > 0.2f) {
            vehicle.engineStartOver = true
        }

        if (data.get(VehicleEntity.POWER) < 0.0004f) {
            vehicle.engineStart = false
            vehicle.engineStartOver = false
        }
    }

    @JvmStatic
    fun tomEngine(vehicle: VehicleEntity, engineInfo: Tom6) {
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val pitchSpeed = engineInfo.pitchSpeed
        val yawSpeed = engineInfo.yawSpeed
        val rollSpeed = engineInfo.rollSpeed
        val lift = engineInfo.liftSpeed
        val speedRate = engineInfo.speedRate
        val data = vehicle.getEntityData()
        val energyCost = (engineInfo.energyCostRate * Mth.abs(data.get(VehicleEntity.POWER))).toInt()

        val f = Mth.clamp(
            Math.max(
                (if (vehicle.onGround()) 0.819f else 0.82f) - 0.005 * vehicle.deltaMovement.length(), 0.5
            ) + 0.001f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90, 0.01, 0.99
        ).toFloat()

        val forward = vehicle.deltaMovement.dot(vehicle.getViewVector(1f)) > 0
        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                vehicle.getViewVector(1f)
                    .scale((if (forward) 0.227 else 0.1) * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
            )
        )
        vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f.toDouble(), f.toDouble(), f.toDouble()))

        if (vehicle.isInFluidType && vehicle.tickCount % 4 == 0) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.6, 0.6, 0.6))
            if (vehicle.lastTickSpeed > 0.4) {
                vehicle.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        vehicle,
                        if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                    ), (20 * ((vehicle.lastTickSpeed - 0.4) * (vehicle.lastTickSpeed - 0.4))).toFloat()
                )
            }
        }

        val passenger = vehicle.getFirstPassenger()

        if (passenger == null || vehicle.isInFluidType) {
            vehicle.setLeftInputDown(false)
            vehicle.setRightInputDown(false)
            vehicle.setForwardInputDown(false)
            vehicle.setBackInputDown(false)
            data.set(
                VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.95f
            )
            if (vehicle.onGround()) {
                vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.94, 1.0, 0.94))
            } else {
                vehicle.xRot = Mth.clamp(vehicle.xRot + 0.1f, -89f, 89f)
            }
        } else if (passenger is Player) {
            if (vehicle.energy > energyCost) {
                if (!vehicle.engineStart && vehicle.forwardInputDown()) {
                    vehicle.engineStart = true
                    if (data.get(VehicleEntity.POWER) > 0) {
                        vehicle.level()
                            .playSound(null, vehicle, engineInfo.engineStartSound, vehicle.soundSource, 3f, 1f)
                    }
                }

                if (vehicle.forwardInputDown()) {
                    data.set(
                        VehicleEntity.POWER, Mth.clamp(
                            (data.get(
                                VehicleEntity.POWER
                            ) + 0.045f * powerAdd).toDouble(), -0.1, 1.0
                        ).toFloat()
                    )
                }

                if (vehicle.backInputDown()) {
                    if (vehicle.onGround()) {
                        vehicle.setDeltaMovement(vehicle.deltaMovement.scale(0.97))
                    }
                    data.set(
                        VehicleEntity.POWER, Math.max(
                            data.get(
                                VehicleEntity.POWER
                            ) - 0.06f * powerReduce, if (vehicle.onGround()) -0.6f else 0.2f
                        )
                    )
                }
            }

            val diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger.getYHeadRot() - vehicle.yRot))
            val diffX = Math.clamp(-60f, 60f, Mth.wrapDegrees(passenger.xRot - vehicle.xRot))

            val roll = Mth.abs(Mth.clamp(vehicle.roll / 60, -1.5f, 1.5f))

            val addY = Mth.clamp(
                Math.min(
                    (if (vehicle.onGround()) 1.5f else 0.9f) * Math.max(
                        vehicle.deltaMovement.length() - 0.06,
                        0.1
                    ).toFloat(), 0.9f
                ) * diffY - 0.5f * data.get(
                    VehicleEntity.DELTA_ROT
                ), -3 * (roll + 1), 3 * (roll + 1)
            )
            val addX = Mth.clamp(
                Math.min(Math.max(vehicle.deltaMovement.length() - 0.1, 0.01).toFloat(), 0.9f) * diffX,
                -4f,
                4f
            )
            val addZ = data
                .get<Float>(VehicleEntity.DELTA_ROT) - (if (vehicle.onGround()) 0f else 0.01f) * diffY * vehicle.deltaMovement
                .length().toFloat()

            val i = vehicle.xRot / 90

            val yRotSync = addY * (1 - Mth.abs(i)) + addZ * i

            vehicle.yRot = vehicle.yRot + yRotSync * yawSpeed
            vehicle.xRot = Mth.clamp(
                vehicle.xRot + addX * pitchSpeed,
                (if (vehicle.onGround()) -12 else -120).toFloat(),
                (if (vehicle.onGround()) 3 else 120).toFloat()
            )
            vehicle.setZRot(vehicle.roll - addZ * (1 - Mth.abs(i)) * rollSpeed)

            if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
                data.set(
                    VehicleEntity.POWER, data.get(
                        VehicleEntity.POWER
                    ) * 0.995f
                )
            }

            if (!vehicle.onGround()) {
                if (vehicle.rightInputDown()) {
                    data.set(
                        VehicleEntity.DELTA_ROT, data.get(
                            VehicleEntity.DELTA_ROT
                        ) - 0.6f
                    )
                } else if (vehicle.leftInputDown()) {
                    data.set(
                        VehicleEntity.DELTA_ROT, data.get(
                            VehicleEntity.DELTA_ROT
                        ) + 0.6f
                    )
                }
            }
        }

        vehicle.consumeEnergy(energyCost)

        // 自动回正
        if (!vehicle.onGround()) {
            val xSpeed = 1 + 20 * Mth.abs(vehicle.xRot / 180)
            val speed = Mth.clamp(Mth.abs(vehicle.roll) / (90 / xSpeed), 0f, 1f)

            if (vehicle.roll > 0) {
                vehicle.setZRot(vehicle.roll - Math.min(speed, vehicle.roll))
            } else if (vehicle.roll < 0) {
                vehicle.setZRot(vehicle.roll + Math.min(speed, -vehicle.roll))
            }
        }

        data.set(
            VehicleEntity.DELTA_ROT, data.get(VehicleEntity.DELTA_ROT) * 0.85f
        )
        if (vehicle.onGround()) {
            data.set(
                VehicleEntity.POWER, data.get(VehicleEntity.POWER) * 0.995f
            )
        }

        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                vehicle.getUpVec(1f).scale(
                    vehicle.deltaMovement
                        .dot(vehicle.getViewVector(1f)) * 0.022 * lift * (1 + Math.sin((if (vehicle.onGround()) 25 else 30) * Mth.DEG_TO_RAD))
                )
            )
        )
        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(
                vehicle.getViewVector(1f).scale(
                    0.02 * speedRate * data.get(
                        VehicleEntity.POWER
                    ) * (if (vehicle.sprintInputDown()) 2.2 else 1.0)
                )
            )
        )
    }

    @JvmStatic
    fun wheelChairEngine(vehicle: VehicleEntity, engineInfo: WheelChair) {
        val buoyancy = engineInfo.buoyancy
        val data = vehicle.getEntityData()
        val energyCost = (engineInfo.energyCostRate * Mth.abs(data.get(VehicleEntity.POWER))).toInt()
        val wheelRotSpeed = engineInfo.wheelRotSpeed
        val wheelDifferential = engineInfo.wheelDifferential.toFloat()
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val steeringSpeed = engineInfo.steeringSpeed
        val bodyRollRate = engineInfo.bodyRollRate.toFloat()
        val jumpEnergyCost = engineInfo.jumpEnergyCost

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(vehicle)
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(0.0, fluidFloat, 0.0))
        }

        if (vehicle.onGround()) {
            val f0 = 0.63f + 0.25f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.05 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble()))
        } else if (vehicle.isInFluidType) {
            val f1 = 0.74f + 0.09f * Mth.abs(
                90 - VehicleVecUtils.calculateAngle(
                    vehicle.deltaMovement,
                    vehicle.getViewVector(1f)
                ).toFloat()
            ) / 90
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).normalize()
                        .scale(0.04 * vehicle.deltaMovement.dot(vehicle.getViewVector(1f)))
                )
            )
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble()))
        } else {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.99, 0.99, 0.99))
        }
        vehicle.isSprinting = vehicle.deltaMovement.horizontalDistance() > 0.15

        val passenger0 = vehicle.getFirstPassenger()
        var diffY = 0f

        if (passenger0 == null) {
            vehicle.setLeftInputDown(false)
            vehicle.setRightInputDown(false)
            vehicle.setForwardInputDown(false)
            vehicle.setBackInputDown(false)
            data.set(VehicleEntity.POWER, 0f)
        } else {
            diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger0.yHeadRot - vehicle.yRot))
            vehicle.yRot += Mth.clamp(0.4f * diffY, -5f * steeringSpeed, 5f * steeringSpeed)

            val direct = (90 - VehicleVecUtils.calculateAngle(vehicle.deltaMovement, vehicle.getViewVector(1f))
                .toFloat()) / 90
            vehicle.setZRot(
                (vehicle.roll + direct * diffY * 0.1 * bodyRollRate * vehicle.deltaMovement.length()).toFloat()
            )
        }

        if (vehicle.forwardInputDown()) {
            if (vehicle.energy <= 0 && passenger0 is Player) {
                moveWithOutPower(vehicle, passenger0, true)
            } else {
                data.set(
                    VehicleEntity.POWER, Math.min(
                        data.get(
                            VehicleEntity.POWER
                        ) + (if (data
                                .get<Float>(VehicleEntity.POWER) < 0
                        ) powerAdd * 2f else powerAdd), (if (vehicle.sprintInputDown()) 2f else 1f)
                    )
                )
            }
        }

        if (vehicle.backInputDown()) {
            if (vehicle.energy <= 0 && passenger0 is Player) {
                moveWithOutPower(vehicle, passenger0, false)
            } else {
                data.set(
                    VehicleEntity.POWER, Math.max(
                        data.get(
                            VehicleEntity.POWER
                        ) - (if (data
                                .get<Float>(VehicleEntity.POWER) > 0
                        ) powerReduce * 2f else powerReduce), -1f
                    )
                )
            }
        }

        if (data.get(VehicleEntity.POWER) > 0) {
            vehicle.targetSpeed = (maxForwardSpeedRate * (1 + vehicle.xRot / 55)).toDouble()
        } else {
            vehicle.targetSpeed = (maxBackwardSpeedRate * (1 - vehicle.xRot / 55)).toDouble()
        }

        if (!vehicle.forwardInputDown() && !vehicle.backInputDown()) {
            data.set(
                VehicleEntity.POWER, data.get(
                    VehicleEntity.POWER
                ) * 0.96f
            )
        }

        if (vehicle.upInputDown() && vehicle.onGround() && vehicle.energy > jumpEnergyCost && vehicle.jumpCoolDown == 0 && engineInfo.canJump) {
            if (passenger0 is ServerPlayer) {
                passenger0.level().playSound(
                    null,
                    passenger0.onPos,
                    ModSounds.WHEEL_CHAIR_JUMP.get(),
                    SoundSource.PLAYERS,
                    1f,
                    1f
                )
            }
            vehicle.consumeEnergy(jumpEnergyCost)
            vehicle.setDeltaMovement(vehicle.deltaMovement.add(vehicle.getUpVec(1f).scale(engineInfo.jumpForce)))
            vehicle.jumpCoolDown = engineInfo.jumpCoolDown
        }

        if (vehicle.level() is ServerLevel) {
            vehicle.consumeEnergy(energyCost)
        }

        val s0 = vehicle.deltaMovement.dot(vehicle.getViewVector(1f))
        vehicle.leftWheelRot =
            (vehicle.leftWheelRot - 1.25 * wheelRotSpeed * s0).toFloat() - 0.015f * wheelDifferential * Mth.clamp(
                0.4f * diffY,
                -5f,
                5f
            )
        vehicle.rightWheelRot =
            (vehicle.rightWheelRot - 1.25 * wheelRotSpeed * s0).toFloat() + 0.015f * wheelDifferential * Mth.clamp(
                0.4f * diffY,
                -5f,
                5f
            )

        if (vehicle.isInFluidType || vehicle.onGround()) {
            val water =
                (if (!vehicle.isInFluidType && !vehicle.onGround()) 0.05f else (if (vehicle.isInFluidType && !vehicle.onGround()) 0.3f else 1f)).toDouble()
            vehicle.setDeltaMovement(
                vehicle.deltaMovement.add(
                    vehicle.getViewVector(1f).scale(
                        0.08 * water * vehicle.targetSpeed * data.get(
                            VehicleEntity.POWER
                        )
                    )
                )
            )
        }
    }

    fun moveWithOutPower(vehicle: VehicleEntity, player: Player, forward: Boolean) {
        vehicle.setDeltaMovement(
            vehicle.deltaMovement.add(vehicle.getViewVector(1f).scale((if (forward) 0.1f else -0.1f).toDouble()))
        )
        if (player is ServerPlayer) {
            player.level().playSound(null, player.onPos, SoundEvents.BOAT_PADDLE_LAND, SoundSource.PLAYERS, 1f, 1f)
        }
        player.causeFoodExhaustion(0.03f)

        vehicle.setForwardInputDown(false)
        vehicle.setBackInputDown(false)
    }

    /**
     * 查找实体下方半球区域内最近的降落辅助方块位置
     *
     * @param radius 搜索半径
     * @return 辅助方块顶面位置，如果未找到则返回null
     */
    fun findNearestLandingPos(entity: VehicleEntity, radius: Int): Vec3? {
        val world = entity.level()
        val entityPos = entity.blockPosition()
        val landingBlocks: MutableList<BlockPos?> = ArrayList<BlockPos?>()

        // 遍历半球区域内的所有方块
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                for (y in -radius..0) { // 只检查实体下方的区域
                    // 检查是否在半球内 (x² + y² + z² ≤ r²)
                    if (x * x + y * y + z * z <= radius * radius) {
                        val checkPos = entityPos.offset(x, y, z)

                        // 检查是否为降落辅助方块
                        if (world.getBlockState(checkPos).`is`(ModTags.Blocks.AUTO_LANDING)) {
                            landingBlocks.add(checkPos)
                        }
                    }
                }
            }
        }

        // 如果没有找到降落辅助方块，返回null
        if (landingBlocks.isEmpty()) {
            return null
        }

        // 按距离排序，找到最近的降落辅助方块
        landingBlocks.sortWith(Comparator.comparingDouble { pos ->
            entity.position().distanceToSqr(pos!!.x + 0.5, (pos.y + 1).toDouble(), pos.z + 0.5)
        })

        return landingBlocks[0]?.center
    }

    fun updateAutoLanding(entity: VehicleEntity, landingTarget: Vec3) {
        // 计算水平方向上的偏移向量 (忽略Y轴)
        val currentPos = entity.position()
        val horizontalOffset = Vec3(
            landingTarget.x - currentPos.x,
            0.0,
            landingTarget.z - currentPos.z
        )

        entity.setDeltaMovement(entity.deltaMovement.multiply(0.975, 0.99, 0.975))

        // 计算距离和方向
        val horizontalDistance = horizontalOffset.length()
        val horizontalDirection = if (horizontalDistance > 0) horizontalOffset.normalize() else Vec3.ZERO


        // 倾斜平滑因子
        val tiltSmoothingFactor = 0.1f

        val horizontalDistanceNew = horizontalDistance - 5 * entity.deltaMovement.horizontalDistance()

        // 计算需要的倾斜角度 (与距离成正比，但有最大限制)
        // 直升机辅助降落这一块
        // 最大倾斜角度(度)
        val maxTiltAngle = 15.0f
        val targetTilt = Math.min(maxTiltAngle.toDouble(), horizontalDistanceNew * 2).toFloat()

        // 将世界方向转换为本地倾斜方向
        // 需要考虑直升机的当前偏航角(yRot)
        val yawRad = Math.toRadians(-entity.yRot)
        val localDirection = Vec3(
            horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
            0.0,
            horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        )

        // 计算目标俯仰和滚转
        val targetXRot = (-localDirection.z * targetTilt).toFloat()
        val targetZRot = (localDirection.x * targetTilt).toFloat()

        // 平滑过渡到目标姿态
        entity.xRot = lerpAngle(entity.xRot, -targetXRot, tiltSmoothingFactor)
        entity.setZRot(lerpAngle(entity.roll, -targetZRot, tiltSmoothingFactor))
    }

    // 角度线性插值方法
    @JvmStatic
    fun lerpAngle(current: Float, target: Float, factor: Float): Float {
        // 处理角度环绕
        var diff = target - current
        while (diff < -180) diff += 360f
        while (diff > 180) diff -= 360f

        return current + diff * factor
    }
}
