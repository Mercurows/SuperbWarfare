package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.VectorTool.calculateY
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import org.joml.Math
import kotlin.math.*

object VehicleEngineUtils {
    @JvmStatic
    fun VehicleEntity.trackEngine(engineInfo: EngineInfo.Track) {
        val buoyancy = engineInfo.buoyancy
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()
        val wheelRotSpeed = engineInfo.wheelRotSpeed
        val wheelDifferential = engineInfo.wheelDifferential
        val trackSpeed = engineInfo.trackRotSpeed
        val trackDifferential = engineInfo.trackDifferential
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        var powerAdd = engineInfo.increment
        var powerReduce = engineInfo.decrement
        var steeringSpeed = engineInfo.steeringSpeed

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(this)
            deltaMovement = deltaMovement.add(0.0, fluidFloat, 0.0)
        }

        val rightDrift = if (drift() && rightInputDown) 0f else 1f
        val leftDrift = if (drift() && leftInputDown) 0f else 1f

        if (onGround()) {

            var f0 = (if (drift()) 0.95f
            else 0.54f + 0.25f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat()))

            if (isInFluidType) {
                f0 -= 3f * VehicleVecUtils.getSubmergedHeight(this).toFloat() * deltaMovement.lengthSqr().toFloat()
            }

            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale((if (drift()) 0.001 else 0.05) * deltaMovement.dot(getViewVector(1f)))
            )

            deltaMovement = deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble())

        } else if (isInFluidType) {

            powerAdd *= 0.1f
            powerReduce *= 0.1f

            val f1 = Mth.clamp(0.9f
                    + 0.09f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat())
                    - 4f * deltaMovement.lengthSqr().toFloat()
                - VehicleVecUtils.getSubmergedHeight(this).toFloat() * 0.02f
                , 0f, 0.99f)

            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale(0.04 * deltaMovement.dot(getViewVector(1f)))
            )
            deltaMovement = deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble())
        } else {
            deltaMovement = deltaMovement.multiply(0.99, 0.99, 0.99)
        }

        if (level().isClientSide) {
            if (isInFluidType && deltaMovement.horizontalDistanceSqr() > 0.3162) {
                addRandomParticle(ParticleTypes.CLOUD, position().add(
                    0.0,
                    VehicleVecUtils.getSubmergedHeight(this) - 0.2,
                    0.0),
                    1f, level(), 0f, (2 + 4 * deltaMovement.length()).toInt())

                addRandomParticle(ParticleTypes.BUBBLE_COLUMN_UP, position().add(
                    0.0,
                    VehicleVecUtils.getSubmergedHeight(this) - 0.2,
                    0.0), 1f, level(), 0f, (2 + 10 * deltaMovement.length()).toInt())

            }
        }

        val passenger0 = getFirstPassenger()

        if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
            forwardInputDown = false
            backInputDown = false
            leftInputDown = false
            rightInputDown = false
            power *= 0.95f
        }

        if (passenger0 == null) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
            power = 0f
        }

        val maxPower = if (sprintInputDown) 1.25f else (if (power > 1) power - 0.002f else 1f)

        if (forwardInputDown && !backInputDown) {
            power = Math.min(power + (if (power < 0) powerAdd * 2f else powerAdd) * (maxPower - (Mth.abs(power) / 1.02f)), maxPower)
        }

        if (backInputDown) {
            power = Math.max(power - (if (power > 0) powerReduce * 4f else powerReduce) * (maxPower - (Mth.abs(power) / 1.02f)), -1f)
            if (rightInputDown) {
                holdTick++
                deltaRot += steeringSpeed * 0.12f * Math.min(holdTick, 10)
            } else if (leftInputDown) {
                holdTick++
                deltaRot -= steeringSpeed * 0.12f * Math.min(holdTick, 10)
            } else {
                holdTick = 0
            }
        } else {
            if (rightInputDown) {
                holdTick++
                deltaRot -= steeringSpeed * 0.12f * Math.min(holdTick, 10)
            } else if (leftInputDown) {
                holdTick++
                deltaRot += steeringSpeed * 0.12f * Math.min(holdTick, 10)
            } else {
                holdTick = 0
            }
        }

        targetSpeed = if (power > 0) {
            (maxForwardSpeedRate * (1 + xRot / 60)).toDouble()
        } else {
            (maxBackwardSpeedRate * (1 - xRot / 60)).toDouble()
        }

        if (!forwardInputDown && !backInputDown) {
            power *= 0.96f
        }

        if (upInputDown) {
            power *= if (isInFluidType) 0.97f else (if (drift()) 0.96f else 0.6f)
        }

        if (rightInputDown || leftInputDown) {
            power *= 0.995f
        }

        if (level() is ServerLevel) {
            consumeEnergy(energyCost)
        }

        if (drift()) {
            steeringSpeed *= 3.4f
        }

        deltaRot *= Math.max(0.76f - 0.1f * deltaMovement.horizontalDistance(), 0.3).toFloat()

        val s0 = deltaMovement.dot(getViewVector(1f))

        leftWheelRot = ((leftWheelRot - wheelRotSpeed * s0 * leftDrift) + Mth.clamp(
            wheelDifferential * deltaRot * leftDrift, -5.0, 5.0
        )).toFloat()
        rightWheelRot = ((rightWheelRot - wheelRotSpeed * s0 * rightDrift) - Mth.clamp(
            wheelDifferential * deltaRot * rightDrift, -5.0, 5.0
        )).toFloat()

        leftTrack = ((leftTrack - trackSpeed * java.lang.Math.PI * s0 * leftDrift) + Mth.clamp(
            trackDifferential * java.lang.Math.PI * deltaRot * leftDrift, -5.0, 5.0
        )).toFloat()
        rightTrack = ((rightTrack - trackSpeed * java.lang.Math.PI * s0 * rightDrift) - Mth.clamp(
            trackDifferential * java.lang.Math.PI * deltaRot * rightDrift, -5.0, 5.0
        )).toFloat()

        val i: Int
        if (leftWheelDamaged && rightWheelDamaged) {
            power *= 0.93f
            i = 0
        } else if (leftWheelDamaged) {
            power *= 0.975f
            i = 3
        } else if (rightWheelDamaged) {
            power *= 0.975f
            i = -3
        } else {
            i = 0
        }

        if (mainEngineDamaged) {
            power *= 0.96f
        }

        yRot = (yRot - (if (isInFluidType && !onGround()) 2.5 else 8.0) * deltaRot - i * s0).toFloat()

        if (isInFluidType || onGround()) {
            deltaMovement = deltaMovement.add(getViewVector(1f).scale((if (drift()) 0.03 else 0.15) * targetSpeed * power))
        }
    }

    @JvmStatic
    fun VehicleEntity.wheelEngine(engineInfo: EngineInfo.Wheel) {
        val buoyancy = engineInfo.buoyancy
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()
        val wheelRotSpeed = engineInfo.wheelRotSpeed
        val wheelDifferential = engineInfo.wheelDifferential
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        var powerAdd = engineInfo.increment
        var powerReduce = engineInfo.decrement
        var steeringSpeed = engineInfo.steeringSpeed

        val level = level()

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(this)
            deltaMovement = deltaMovement.add(0.0, fluidFloat, 0.0)
        }

        if (onGround()) {
            var f0 = (if (drift()) 0.96f
            else 0.54f + 0.25f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat()))

            if (isInFluidType) {
                f0 -= 3f * VehicleVecUtils.getSubmergedHeight(this).toFloat() * deltaMovement.lengthSqr().toFloat()
            }

            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale((if (drift()) 0.001 else 0.05) * deltaMovement.dot(getViewVector(1f)))
            )

            deltaMovement = deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble())
        } else if (isInFluidType) {
            powerAdd *= 0.1f
            powerReduce *= 0.1f

            val f1 = Mth.clamp(0.9f
                    + 0.09f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat())
                    - 4f * deltaMovement.lengthSqr().toFloat()
                    - VehicleVecUtils.getSubmergedHeight(this).toFloat() * 0.02f
                , 0f, 0.99f)

            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale(0.04 * deltaMovement.dot(getViewVector(1f)))
            )
            deltaMovement = deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble())
        } else {
            deltaMovement = deltaMovement.multiply(0.99, 0.99, 0.99)
        }

        if (level.isClientSide) {
            if (isInFluidType && deltaMovement.horizontalDistanceSqr() > 0.3162) {
                addRandomParticle(ParticleTypes.CLOUD, position().add(
                    0.0,
                    VehicleVecUtils.getSubmergedHeight(this) - 0.2,
                    0.0),
                    1f, level(), 0f, (2 + 4 * deltaMovement.length()).toInt())

                addRandomParticle(ParticleTypes.BUBBLE_COLUMN_UP, position().add(
                    0.0,
                    VehicleVecUtils.getSubmergedHeight(this) - 0.2,
                    0.0), 1f, level(), 0f, (2 + 10 * deltaMovement.length()).toInt())

            }

            if (upInputDown && onGround() && deltaMovement.horizontalDistanceSqr() > 0.01) {
                for (pos in computed().terrainCompat) {
                    val worldPosition = transformPosition(
                        getVehicleTransform(1f),
                        pos.x, pos.y, pos.z
                    )

                    val option = CustomCloudOption(0x000000, 200, 1.5f, 0f, false, false)

                    level().addParticle(option,
                        worldPosition.x,
                        worldPosition.y,
                        worldPosition.z,
                        0.0, 0.0, 0.0
                    )
                }
            }
        }

        val passenger0 = getFirstPassenger()

        if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
            forwardInputDown = false
            backInputDown = false
            leftInputDown = false
            rightInputDown = false
            power *= 0.95f
            deltaRot *= 0.5f
        }

        if (passenger0 == null) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
            power = 0f
        }

        val maxPower = if (sprintInputDown) 1.3f else (if (power > 1) power - 0.002f else 1f)

        if (forwardInputDown && !backInputDown) {
            power = Math.min(power + (if (power < 0) powerAdd * 2f else powerAdd) * (maxPower - (Mth.abs(power) / 1.02f)), maxPower)
        }

        if (backInputDown) {
            power = Math.max(
                power - (if (power > 0) powerReduce * 4f else powerReduce) * (maxPower - (Mth.abs(power) / 1.02f)), -1f
            )
        }

        targetSpeed = if (power > 0) {
            (maxForwardSpeedRate * (1 + xRot / 60)).toDouble()
        } else {
            (maxBackwardSpeedRate * (1 - xRot / 60)).toDouble()
        }

        if (!forwardInputDown && !backInputDown) {
            power *= 0.97f
        }

        if (upInputDown) {
            power *= if (isInFluidType) 0.97f else (if (drift()) 0.93f else 0.6f)
        }

        if (rightInputDown || leftInputDown) {
            power *= 0.995f
        }

        if (level is ServerLevel) {
            consumeEnergy(energyCost)
        }

        val i: Int
        if (leftWheelDamaged && rightWheelDamaged) {
            power *= 0.93f
            i = 0
        } else if (leftWheelDamaged) {
            power *= 0.975f
            i = 3
        } else if (rightWheelDamaged) {
            power *= 0.975f
            i = -3
        } else {
            i = 0
        }

        if (mainEngineDamaged) {
            power *= 0.875f
        }

        if (drift()) {
            steeringSpeed *= 1.5f
        }

        if (rightInputDown) {
            holdTick++
            deltaRot += steeringSpeed * 0.12f * Math.min(holdTick, 10)
        } else if (leftInputDown) {
            holdTick++
            deltaRot -= steeringSpeed * 0.12f * Math.min(holdTick, 10)
        } else {
            holdTick = 0
        }

        deltaRot *= Math.max(0.78f - 0.25f * deltaMovement.horizontalDistance(), 0.1).toFloat()

        val s0 = deltaMovement.dot(getViewVector(1f))

        leftWheelRot = ((leftWheelRot - wheelRotSpeed * s0) - Mth.clamp(
            wheelDifferential * deltaRot, -5.0, 5.0
        ) * deltaMovement.length()).toFloat()
        rightWheelRot = ((rightWheelRot - wheelRotSpeed * s0) + Mth.clamp(
            wheelDifferential * deltaRot, -5.0, 5.0
        ) * deltaMovement.length()).toFloat()

        rudderRot = Mth.clamp(
            rudderRot - deltaRot,
            -0.8f,
            0.8f
        ) * 0.75f

        yRot = (yRot - Math.max(
            (if (isInFluidType && !onGround()) 6 else 12) * deltaMovement
                .horizontalDistance(), 0.0
        ) * rudderRot * (if (power > 0) 1 else -1) - i * s0).toFloat()

        if ((isInFluidType || onGround())) {
            deltaMovement = deltaMovement.add(getViewVector(1f).scale((if (drift()) 0.02 else 0.15) * targetSpeed * power))
        }
    }

    @JvmStatic
    fun VehicleEntity.shipEngine(engineInfo: EngineInfo.Ship) {
        val buoyancy = engineInfo.buoyancy
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val steeringSpeed = engineInfo.steeringSpeed
        val bodyPitchRate = engineInfo.bodyPitchRate

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(this)
            deltaMovement = deltaMovement.add(0.0, fluidFloat, 0.0)
        }

        if (onGround()) {
            deltaMovement = deltaMovement.multiply(0.2, 0.99, 0.2)
        } else if (isInFluidType) {
            val f = (0.835f - 0.04f * min(VehicleVecUtils.getSubmergedHeight(this), bbHeight.toDouble()) + 0.005f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat()))
            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale(0.04 * deltaMovement.dot(getViewVector(1f)))
            )
            deltaMovement = deltaMovement.multiply(f, 0.85, f)
        } else {
            deltaMovement = deltaMovement.multiply(0.99, 0.99, 0.99)
        }

        if (level().isClientSide && isInFluidType && deltaMovement.horizontalDistanceSqr() > 0.3162) {
            val y = y + VehicleVecUtils.getSubmergedHeight(this) - 0.2
            addRandomParticle(ParticleTypes.CLOUD, position().add(
                0.0,
                y,
                0.0),
                1.2f, level(), 0f, (2 + 4 * deltaMovement.length()).toInt())

            addRandomParticle(ParticleTypes.BUBBLE_COLUMN_UP, position().add(
                0.0,
                y,
                0.0), 1.2f, level(), 0f, (2 + 10 * deltaMovement.length()).toInt())

            addRandomParticle(ParticleTypes.BUBBLE_COLUMN_UP, position().add(
                -4.5 * lookAngle.x,
                -0.25,
                -4.5 * lookAngle.z), 0.3f, level(), 0f, (40 * Mth.abs(power)).toInt())

        }

        val passenger0 = getFirstPassenger()

        if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
            forwardInputDown = false
            backInputDown = false
            power *= 0.95f
        }

        if (passenger0 == null) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
        }

        val maxPower = if (sprintInputDown) 1.3f else (if (power > 1) power - 0.002f else 1f)

        if (forwardInputDown && !backInputDown) {
            power = Math.min(power + (if (power < 0) powerAdd * 2f else powerAdd) * (maxPower - (Mth.abs(power) / 1.02f)), maxPower)
        }

        if (backInputDown) {
            power = Math.max(
                    power - (if (power > 0) powerReduce * 4f else powerReduce) * (maxPower - (Mth.abs(power) / 1.02f)), -1f
            )
        }

        targetSpeed = if (power > 0) {
            maxForwardSpeedRate.toDouble()
        } else {
            maxBackwardSpeedRate.toDouble()
        }

        if (!forwardInputDown && !backInputDown) {
            power *= 0.97f
        }

        if (rightInputDown || leftInputDown) {
            power *= 0.98f
        }

        if (mainEngineDamaged) {
            power *= 0.875f
        }

        if (level() is ServerLevel) {
            consumeEnergy(energyCost)
        }

        if (rightInputDown) {
            holdTick++
            deltaRot -= steeringSpeed * 0.03f * Math.min(holdTick, 40)
        } else if (leftInputDown) {
            holdTick++
            deltaRot += steeringSpeed * 0.03f * Math.min(holdTick, 40)
        } else {
            holdTick = 0
        }

        deltaRot *= Math.max(0.78f - 0.25f * deltaMovement.horizontalDistance(), 0.1).toFloat()

        propellerRot += 2 * power
        rudderRot = Mth.clamp(
            rudderRot - deltaRot,
            -0.8f,
            0.8f
        ) * 0.75f

        if (isInFluidType || isUnderWater) {
            xRot *= 0.85f
            val direct = (90 - VehicleVecUtils.calculateAngle(deltaMovement, getViewVector(1f)).toFloat()) / 90
            xRot =
                (xRot - direct * (if (onGround()) 0 else 1) * bodyPitchRate * deltaMovement.horizontalDistance()).toFloat()
            yRot = (yRot - 20 * deltaMovement.horizontalDistance() * deltaRot * (if (power > 0) 1 else -1)).toFloat()
            deltaMovement = deltaMovement.add(
                getViewVector(1f).scale(0.11 * targetSpeed * power * (if (Mth.abs(power) <= 1) Mth.abs(power) else 1f))
            )

            deltaMovement = deltaMovement.add(
                getUpVec(1f).scale(deltaMovement.length() * 0.005 * VehicleVecUtils.getSubmergedHeight(this) * Mth.abs(xRot))
            )
        } else {
            xRot *= 0.99f
        }

        setZRot(roll * 0.95f)
    }

    @JvmStatic
    fun VehicleEntity.helicopterEngine(engineInfo: EngineInfo.Helicopter) {
        val energyCost = engineInfo.energyCostRate.toInt()
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        var pitchSpeed = engineInfo.pitchSpeed
        var yawSpeed = engineInfo.yawSpeed
        var rollSpeed = engineInfo.rollSpeed
        val lift = engineInfo.liftSpeed
        val speed = engineInfo.speed

        if (onGround()) {
            deltaMovement = deltaMovement.multiply(0.8, 1.0, 0.8)
        } else {
            if (!sympatheticDetonated) {
                setZRot(roll * (if (backInputDown) 0.9f else 0.99f))
            }
            val f = Mth.clamp(
                0.93499f - 0.01 * deltaMovement.lengthSqr() + (0.07 * speed) + 0.001f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat()), 0.01, 0.99
            ).toFloat()
            deltaMovement = deltaMovement.add(
                getViewVector(1f).scale(
                    (if (xRot < 0) -0.001 else (if (xRot > 0) 0.001 else 0.0)) * deltaMovement.length()
                )
            )
            deltaMovement = deltaMovement.multiply(f.toDouble(), 0.95, f.toDouble())
        }

        if (isInFluidType && tickCount % 4 == 0 && VehicleVecUtils.getSubmergedHeight(this) > 0.5 * bbHeight) {
            deltaMovement = deltaMovement.multiply(0.6, 0.6, 0.6)
            hurt(
                ModDamageTypes.causeVehicleStrikeDamage(
                    level().registryAccess(),
                    vehicle,
                    if (getFirstPassenger() == null) vehicle else getFirstPassenger()
                ), 6 + (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))).toFloat()
            )
            crash = true
        }

        val pilot = getFirstPassenger()
        val hasPassenger = getPassengers().isNotEmpty()

        val diffX: Float
        val diffZ: Float

        if (health > 0.1f * getMaxHealth()) {
            if (pilot == null) {
                leftInputDown = false
                rightInputDown = false
                forwardInputDown = false
                backInputDown = false
                upInputDown = false
                downInputDown = false

                if (!isWreck) {
                    roll *= 0.99f
                    xRot -= 0.5f * deltaMovement.dot(getViewVector(1f)).toFloat()
                    roll -= 0.5f * deltaMovement.dot(getRightVec(1f)).toFloat()
                }

                if (!hasPassenger && engineStartOver) {
                    power = Math.max(power * 0.99f, if (onGround()) 0f else 0.048f)
                }
            } else {
                if (hoverMode) {
                    roll *= 0.97f
                    roll -= 0.5f * deltaMovement.dot(getRightVec(1f)).toFloat()

                    xRot *= 0.97f
                    xRot -= 0.5f * deltaMovement.dot(getViewVector(1f)).toFloat()

                    rollSpeed *= 0.05f
                    yawSpeed *= 0.5f
                    pitchSpeed *= 0.2f

                    deltaMovement = deltaMovement.multiply(0.95, 1.0, 0.95)
                }

                if (rightInputDown) {
                    holdTick++
                    deltaRot -= 1f * Math.min(holdTick, 7) * power
                } else if (leftInputDown) {
                    holdTick++
                    deltaRot += 1f * Math.min(holdTick, 7) * power
                } else {
                    holdTick = 0
                }
                xRot += (if (onGround()) 0f else 1.5f) * pitchSpeed * mouseMoveSpeedY * synchedPropellerRot
                setZRot(roll - rollSpeed * (deltaRot + (if (onGround()) 0f else 0.25f) * mouseMoveSpeedX * synchedPropellerRot))

                yRot += yawSpeed * Mth.clamp(
                    (if (onGround()) 0.1f else 2f) * mouseMoveSpeedX * synchedPropellerRot + (if (subEngineDamaged) 25 else 0) * synchedPropellerRot,
                    -10f,
                    10f
                )

                if (onGround()) {
                    hoverMode = false
                    setZRot(roll * 0.98f)
                    xRot *= 0.98f
                }
            }

            if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
                power *= 0.995f
                forwardInputDown = false
                backInputDown = false
                engineStart = false
                engineStartOver = false
            } else {
                val up = forwardInputDown
                val down = downInputDown

                if (upInputDown) {
                    upInputDown = false
                    hoverMode = !hoverMode
                }

                if (!engineStart && up) {
                    engineStart = true
                    level().playSound(null, this, engineInfo.engineStartSound, soundSource, 3f, 1f)
                }

                if (up && engineStartOver) {
                    holdPowerTick++
                    power = Math.min(power + 0.0007f * powerAdd * Math.min(holdPowerTick, 10), 0.12f)
                }

                if (engineStartOver) {
                    if (down) {
                        holdPowerTick++
                        power = Math.max(
                            power - 0.001f * powerReduce * Math.min(holdPowerTick, 5),
                            if (onGround()) 0f else 0.035f / lift
                        )
                    } else if (backInputDown) {
                        holdPowerTick++
                        power = Math.max(
                            power - 0.001f * powerReduce * Math.min(holdPowerTick, 5),
                            if (onGround()) 0f else 0.058f / lift
                        )
                    }
                }

                if (engineStart && !engineStartOver) {
                    power = Math.min(power + 0.0012f * powerAdd, 0.045f)
                }

                if (!(up || down || backInputDown) && engineStartOver) {
                    val force = (if (hoverMode) 0.01f else 0.002f) * deltaMovement.y().toFloat()
                    power = if (deltaMovement.y() < 0) {
                        Math.min(power - force, 0.12f)
                    } else {
                        Math.max(power - (if (onGround()) 0.25f * force else force), 0f)
                    }
                    holdPowerTick = 0
                }
            }
        } else if (engineStartOver) {
            power = Math.max(power - (if (isWreck) 0.0006f else 0.0003f), if (onGround()) 0f else 0.04f)

            if (!onGround()) {
                destroyRot += if (isWreck) 0.2f else 0.06f
            }

            destroyRot *= if (onGround()) 0.99f else 0.994f

            diffX = -15 - xRot
            diffZ = -35 - roll

            xRot += diffX * 0.2f * synchedPropellerRot
            yRot += destroyRot
            setZRot(roll + diffZ * 0.75f * synchedPropellerRot)
            deltaMovement = deltaMovement.add(getViewVector(1f).scale(-0.006f * destroyRot.toDouble()))
        }

        if (mainEngineDamaged) {
            power *= 0.98f
        }

        deltaRot *= 0.9f
        synchedPropellerRot = Mth.lerp(0.18f, synchedPropellerRot, power)
        propellerRot += 30 * synchedPropellerRot
        synchedPropellerRot *= 0.9995f

        if (engineStart) {
            consumeEnergy(
                (energyCost * 8.3333f * Mth.abs(power)).toInt()
            )
        }

        deltaMovement = deltaMovement.add(
            getUpVec(1f).scale((synchedPropellerRot * lift * 0.66f).toDouble())
        )

        if (power > 0.04f) {
            engineStart = true
            engineStartOver = true
        }

        if (power < 0.0004f) {
            engineStart = false
            engineStartOver = false
        }
    }

    @JvmStatic
    fun VehicleEntity.aircraftEngine(engineInfo: EngineInfo.Aircraft) {
        var powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val pitchSpeed = engineInfo.pitchSpeed
        val yawSpeed = engineInfo.yawSpeed
        val rollSpeed = engineInfo.rollSpeed
        val lift = engineInfo.liftSpeed
        val speedRate = engineInfo.speedRate
        val resistance = engineInfo.resistance * if (downInputDown) 1.5 else 1.0
        val gearRotateAngle = engineInfo.gearRotateAngle
        val clampPitch = engineInfo.clampPitch
        val clampRoll = engineInfo.clampRoll
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()

        val speedSqr = deltaMovement.lengthSqr()
        val speed = deltaMovement.length()
        val dotViewVector = deltaMovement.dot(getViewVector(1f))
        val normalizeDotViewVector = deltaMovement.normalize().dot(getViewVector(1f))

        var f = Mth.clamp(0.96 - 0.0017 * resistance * speedSqr - 0.00001 * (1 - Mth.abs(normalizeDotViewVector.toFloat())), 0.01, 0.99).toFloat()

        if (onGround()) {
            if (isWreck) {
                deltaMovement = deltaMovement.multiply(0.9, 1.0, 0.9)
            }
            f = 0.497f + 0.45f * Mth.abs(normalizeDotViewVector.toFloat())
            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale(0.05 * dotViewVector)
            )
        } else {
            val forward = dotViewVector > 0
            deltaMovement = deltaMovement.add(
                getViewVector(1f)
                    .scale((if (forward) 0.04 else -0.04) * dotViewVector)
            )
        }

        deltaMovement = deltaMovement.multiply(f.toDouble(), f.toDouble(), f.toDouble())

        if (isInFluidType && tickCount % 4 == 0) {
            deltaMovement = deltaMovement.multiply(0.6, 0.6, 0.6)
            if (lastTickSpeed > 0.4) {
                hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        level().registryAccess(),
                        this,
                        if (getFirstPassenger() == null) this else getFirstPassenger()
                    ), (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))).toFloat()
                )
            }
            crash = true
        }

        if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
            forwardInputDown = false
            backInputDown = false
            engineStart = false
            engineStartOver = false
            power *= 0.996f
            deltaMovement = deltaMovement.multiply(0.998, 1.0, 0.998)
        } else {
            consumeEnergy(energyCost)
        }

        if (health > 0.1f * getMaxHealth()) {
            if (getPassengers().isEmpty() || isInFluidType) {
                leftInputDown = false
                rightInputDown = false
                forwardInputDown = false
                backInputDown = false
                sprintInputDown = false
                downInputDown = false
                if (onGround()) {
                    power *= 0.95f
                    deltaMovement = deltaMovement.multiply(0.94, 1.0, 0.94)
                } else {
                    power *= 0.995f
                    xRot = Mth.clamp(xRot + 0.1f, -89f, 89f)
                }
            }

            if (firstPassenger != null) {
                if (!engineStart && forwardInputDown && power > 0.01f) {
                    engineStart = true
                    level().playSound(null, this, engineInfo.engineStartSound, soundSource, 3f, 1f)
                }

                if (sprintInputDown || onGround()) {
                    powerAdd *= 1.6f
                }

                if (energy >= energyCost) {
                    val maxPower = if (sprintInputDown || onGround()) 3 else (if (power > 1) power - 0.012 else 1)

                    if (forwardInputDown) {
                        power = Mth.clamp((power + 0.006f * powerAdd).toDouble(), -0.1, maxPower.toDouble()).toFloat() }

                    if (backInputDown) {
                        power = Math.max(
                                power - 0.006f * powerReduce, if (onGround()) -0.2f else 0.025f
                        )
                    }
                }

                if (!onGround()) {
                    if (rightInputDown) {
                        holdTick++
                        deltaRot -= 0.05f * Math.min(holdTick, 12)
                    } else if (leftInputDown) {
                        holdTick++
                        deltaRot += 0.05f * Math.min(holdTick, 12)
                    } else {
                        holdTick = 0
                    }
                } else {
                    holdTick = 0
                }

                // 刹车
                if (downInputDown) {
                    if (onGround()) {
                        power *= 0.92f
                        deltaMovement = deltaMovement.multiply(0.97, 1.0, 0.97)
                    } else {
                        power = Math.max(power * 0.97f, if (onGround()) -0.3f else 0.025f)
                    }

                    planeBreak = Math.min(planeBreak + 10, 60f)
                }
            } else {
                leftInputDown = false
                rightInputDown = false
                forwardInputDown = false
                backInputDown = false
                sprintInputDown = false
                downInputDown = false
            }

            val rotSpeed = 0.3f + 3.2f * Mth.abs(calculateY(roll))

            var addY = Mth.clamp(Math.max(0.24f * speed.toFloat(), 0f) * mouseMoveSpeedX, -rotSpeed, rotSpeed)
            if (onGround()) {
                addY = Mth.clamp(Mth.clamp(0.6f * speed.toFloat(), 0f, 0.75f) * mouseMoveSpeedX, -1f, 1f)
            }

            val addX = Mth.clamp(Mth.clamp(dotViewVector - 0.24, 0.1, 0.2).toFloat() * mouseMoveSpeedY, -3.5f, 3.5f)
            val addZ = deltaRot - (if (onGround() || Mth.abs(roll) > 60) 0f else 0.02f * (60 - Mth.abs(roll)) / 60) * mouseMoveSpeedX * dotViewVector.toFloat()

            yRot += yawSpeed * addY
            if (!onGround()) {
                xRot = Mth.clamp(xRot + pitchSpeed * addX, -clampPitch, clampPitch)

                if (tickCount > 5) {
                    updateRotation(this)
                }

                if ((roll > 0 && addY < 0) || (roll < 0 && addY > 0)) {
                    addY *= Mth.clamp(1f - Mth.abs(roll) / 45, 0f, 1f)
                }

                setZRot(Mth.clamp(roll - rollSpeed * addZ, -clampRoll, clampRoll))
            }

            // 自动回正
            if (!onGround()) {
                val xSpeed = 1 - Mth.clamp((Mth.abs(xRot) - 60) / 90, 0f, 1f)
                val zSpeed = Mth.clamp(Mth.abs(roll) / 90, 0f, 1f)

                if (roll > 0) {
                    setZRot(roll - Math.min(zSpeed, roll))
                } else if (roll < 0) {
                    setZRot(roll + Math.min(zSpeed, -roll))
                }

                roll *= xSpeed
            }

            if (Mth.abs(xRot) < 20 && Mth.abs(mouseMoveSpeedY) < 0.002) {
                xRot += if (deltaMovement.y() < 0) {
                    1.2f * deltaMovement.y.toFloat()
                } else {
                    0.5f * deltaMovement.y.toFloat()
                }
                xRot *= 0.996f
            }

            propellerRot += 30 * power

            // 起落架
            if (engineInfo.hasGear) {
                if (upInputDown) {
                    upInputDown = false
                    if (synchedGearRot == 0f && !onGround()) {
                        gearUp = true
                    } else if (synchedGearRot == 1f) {
                        gearUp = false
                    }
                }

                if (onGround()) {
                    gearUp = false
                }

                synchedGearRot = if (gearUp) {
                    Math.min(synchedGearRot + 0.05f, 1f)
                } else {
                    Math.max(synchedGearRot - 0.05f, 0f)
                }

                gearRot = synchedGearRot * gearRotateAngle
            }

            val flapX =
                (1 - (Mth.abs(roll)) / 90) * Mth.clamp(mouseMoveSpeedY * 3, -15f, 15f) - calculateY(
                    roll
                ) * Mth.clamp(mouseMoveSpeedX * 3, -15f, 15f)

            flap1LRot = Mth.clamp(
                -flapX - 15 * addZ - planeBreak,
                -15f,
                15f
            )
            flap1RRot = Mth.clamp(
                -flapX + 15 * addZ - planeBreak,
                -15f,
                15f
            )
            flap1L2Rot = Mth.clamp(
                -flapX - 15 * addZ + planeBreak,
                -15f,
                15f
            )
            flap1R2Rot = Mth.clamp(
                -flapX + 15 * addZ + planeBreak,
                -15f,
                15f
            )

            flap2LRot = Mth.clamp(flapX - 15 * addZ, -15f, 15f)
            flap2RRot = Mth.clamp(flapX + 15 * addZ, -15f, 15f)

            val flapY =
                (1 - (Mth.abs(roll)) / 90) * Mth.clamp(mouseMoveSpeedX * 3, -15f, 15f) + calculateY(
                    roll
                ) * Mth.clamp(mouseMoveSpeedY * 3, -15f, 15f)
            flap3Rot = flapY * 5
        } else {
            power = Math.max(power - if (onGround()) 0.03f else 0.0003f, if (onGround()) 0f else 0.02f)
            if (onGround()) {
                destroyRot *= 0.95f
            } else {
                destroyRot += 0.1f
            }

            val diffX: Float = 90 - xRot
            xRot += diffX * 0.001f * destroyRot
            setZRot(roll - destroyRot)
            deltaMovement = deltaMovement.add(0.0, -0.03, 0.0)
            deltaMovement = deltaMovement.add(0.0, -destroyRot * 0.005, 0.0)
        }

        deltaRot *= 0.9f
        planeBreak *= 0.8f
        if (onGround()) {
            power *= 0.995f
        }

        if (mainEngineDamaged) {
            power *= 0.96f
        }

        if (subEngineDamaged) {
            power *= 0.96f
        }

        val flapAngle = ((flap1LRot + flap1RRot + flap1L2Rot + flap1R2Rot) / 50).toDouble()

        deltaMovement = deltaMovement.add(
            getUpVec(1f).scale(
                (1 - Mth.abs(deltaMovement.normalize().dot(getUpVec(1f)).toFloat())) * speed * 0.008 * lift * (flapAngle + Mth.clamp(4 - 0.25 * Mth.abs(dotViewVector.toFloat()), 1.0, 4.0))
            )
        )

        val force = 0.047 * power

        deltaMovement = deltaMovement.add(getViewVector(1f).scale(force * speedRate))
        val vd = 0.05 * deltaMovement.dot(getUpVec(1f).scale(-1.0))
        if (!onGround() && tickCount > 10 && vd > 0.05 && Mth.abs(xRot) < 25) {
            deltaMovement = deltaMovement.add(getViewVector(1f).scale(vd))
        }

        if (power > 0.2f) {
            engineStart = true
            engineStartOver = true
        }

        if (power < 0.0004f) {
            engineStart = false
            engineStartOver = false
        }
    }

    private fun updateRotation(entity: VehicleEntity) {
        val d0 = entity.deltaMovement.add(0.0, -0.06, 0.0)
        if (Mth.abs(entity.xRot) < 60) {
            val diffY = Mth.wrapDegrees(-VehicleVecUtils.getYRotFromVector(d0) - entity.yRot).toFloat()
            entity.yRot += 0.002f * diffY
        }
        if (Mth.abs(entity.xRot) < 90) {
            val diffX = Mth.wrapDegrees(-VehicleVecUtils.getXRotFromVector(d0) - entity.xRot).toFloat()
            entity.xRot += 0.01f * diffX
        }
    }

    @JvmStatic
    fun VehicleEntity.tomEngine(engineInfo: EngineInfo.Tom6) {
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val pitchSpeed = engineInfo.pitchSpeed
        val yawSpeed = engineInfo.yawSpeed
        val rollSpeed = engineInfo.rollSpeed
        val lift = engineInfo.liftSpeed
        val speedRate = engineInfo.speedRate
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()

        val f = Mth.clamp(
            Math.max(
                (if (onGround()) 0.96f else 1f) - 0.015 * deltaMovement.lengthSqr(), 0.5
            ) + 0.0001f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat()), 0.01, 0.99
        ).toFloat()

        val v0 = deltaMovement.normalize().vectorTo(getViewVector(1f))
        deltaMovement = deltaMovement.add(v0.normalize().scale(deltaMovement.length() * 0.05))

        deltaMovement = deltaMovement.multiply(f.toDouble(), f.toDouble(), f.toDouble())

        if (isInFluidType && tickCount % 4 == 0) {
            deltaMovement = deltaMovement.multiply(0.6, 0.6, 0.6)
            if (lastTickSpeed > 0.4) {
                hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        level().registryAccess(),
                        this,
                        if (getFirstPassenger() == null) this else getFirstPassenger()
                    ), (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))).toFloat()
                )
            }
        }

        val passenger = getFirstPassenger()

        if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
            forwardInputDown = false
            backInputDown = false
            engineStart = false
            engineStartOver = false
            power *= 0.996f
        } else {
            consumeEnergy(energyCost)
        }

        if (passenger == null || isInFluidType) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
            power *= 0.95f
            if (onGround()) {
                deltaMovement = deltaMovement.multiply(0.94, 1.0, 0.94)
            } else {
                xRot = Mth.clamp(xRot + 0.1f, -89f, 89f)
            }
        } else if (passenger is Player) {

            val maxPower = if (sprintInputDown || onGround()) 2.2f else (if (power > 1) power - 0.012f else 1f)

            if (forwardInputDown) {
                power = Mth.clamp(power + 0.02f * powerAdd, -0.1f, maxPower)
            }

            if (backInputDown) {
                if (onGround()) {
                    deltaMovement = deltaMovement.scale(0.97)
                }
                power = Math.max(power - 0.01f * powerReduce, if (onGround()) -0.1f else 0.05f)
            }

            val diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger.getYHeadRot() - yRot))
            val diffX = Math.clamp(-60f, 60f, Mth.wrapDegrees(passenger.xRot - xRot))

            val deltaRoll = Mth.abs(Mth.clamp(roll / 60, -1.5f, 1.5f))

            val addY = Mth.clamp(
                Math.min(
                    (if (onGround()) 1.5f else 0.9f) * Math.max(
                        deltaMovement.length() - 0.06,
                        0.1
                    ).toFloat(), 0.9f
                ) * diffY - 0.5f * deltaRot, -3 * (deltaRoll + 1), 3 * (deltaRoll + 1)
            )
            val addX = Mth.clamp(
                Math.min(Math.max(deltaMovement.length() - 0.1, 0.01).toFloat(), 0.9f) * diffX,
                -4f,
                4f
            )
            val addZ = deltaRot - (if (onGround()) 0f else 0.01f) * diffY * deltaMovement
                .length().toFloat()

            val i = xRot / 90

            val yRotSync = addY * (1 - Mth.abs(i)) + addZ * i

            yRot += yRotSync * yawSpeed
            xRot = Mth.clamp(
                xRot + addX * pitchSpeed,
                (if (onGround()) -12 else -120).toFloat(),
                (if (onGround()) 3 else 120).toFloat()
            )
            setZRot(roll - addZ * (1 - Mth.abs(i)) * rollSpeed)

            if (!forwardInputDown && !backInputDown) {
                power *= 0.995f
            }

            if (!onGround()) {
                if (rightInputDown) {
                    holdTick++
                    deltaRot -= 0.04f * Math.min(holdTick, 20)
                } else if (leftInputDown) {
                    deltaRot += 0.04f * Math.min(holdTick, 20)
                }
            }
        }

        consumeEnergy(energyCost)

        // 自动回正
        if (!onGround()) {
            val xSpeed = 1 + 20 * Mth.abs(xRot / 180)
            val speed = Mth.clamp(Mth.abs(roll) / (90 / xSpeed), 0f, 1f)

            if (roll > 0) {
                setZRot(roll - Math.min(speed, roll))
            } else if (roll < 0) {
                setZRot(roll + Math.min(speed, -roll))
            }
        }

        deltaRot *= 0.85f
        if (onGround()) {
            power *= 0.995f
        }

        deltaMovement = deltaMovement.add(
            getUpVec(1f).scale(
                deltaMovement
                    .dot(getViewVector(1f)) * 0.022 * lift * (1 + Math.sin((if (onGround()) 25 else 30) * Mth.DEG_TO_RAD))
            )
        )
        deltaMovement = deltaMovement.add(
            getViewVector(1f).scale(
                0.061 * speedRate * power
            )
        )
    }

    @JvmStatic
    fun VehicleEntity.wheelChairEngine(engineInfo: EngineInfo.WheelChair) {
        val buoyancy = engineInfo.buoyancy
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()
        val wheelRotSpeed = engineInfo.wheelRotSpeed
        val wheelDifferential = engineInfo.wheelDifferential.toFloat()
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val powerAdd = engineInfo.increment
        val powerReduce = engineInfo.decrement
        val steeringSpeed = engineInfo.steeringSpeed
        val jumpEnergyCost = engineInfo.jumpEnergyCost

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(this)
            deltaMovement = deltaMovement.add(0.0, fluidFloat, 0.0)
        }

        if (onGround()) {
            val f0 = 0.63f + 0.25f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat())
            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale(0.05 * deltaMovement.dot(getViewVector(1f)))
            )
            deltaMovement = deltaMovement.multiply(f0.toDouble(), 0.99, f0.toDouble())
        } else if (isInFluidType) {
            val f1 = 0.74f + 0.09f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat())
            deltaMovement = deltaMovement.add(
                getViewVector(1f).normalize()
                    .scale(0.04 * deltaMovement.dot(getViewVector(1f)))
            )
            deltaMovement = deltaMovement.multiply(f1.toDouble(), 0.85, f1.toDouble())
        } else {
            deltaMovement = deltaMovement.multiply(0.99, 0.99, 0.99)
        }
        isSprinting = deltaMovement.horizontalDistance() > 0.15

        val passenger0 = getFirstPassenger()
        var diffY = 0f

        if (passenger0 == null) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
            power = 0f
        } else {
            diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(passenger0.yHeadRot - yRot))
            yRot += Mth.clamp(0.4f * diffY, -5f * steeringSpeed, 5f * steeringSpeed)
        }

        if (forwardInputDown) {
            if ((energy <= energyCost || (maxEnergy > 0 && energy <= 0)) && passenger0 is Player) {
                moveWithOutPower(passenger0, true)
            } else {
                power = Math.min(
                    power + (if (power < 0) powerAdd * 2f else powerAdd) * (1 - (power / 1.02f)), (if (sprintInputDown) 2f else 1f)
                )
            }
        }

        if (backInputDown) {
            if (energy <= 0 && passenger0 is Player) {
                moveWithOutPower(passenger0, false)
            } else {
                power = Math.max(
                    power - (if (power > 0) powerReduce * 2f else powerReduce) * (1 - (power / 1.02f)), -1f
                )
            }
        }

        targetSpeed = if (power > 0) {
            (maxForwardSpeedRate * (1 + xRot / 60)).toDouble()
        } else {
            (maxBackwardSpeedRate * (1 - xRot / 60)).toDouble()
        }

        power *= if (power > 0) {
            1 + xRot / 514
        } else {
            1 - xRot / 514
        }

        if (!forwardInputDown && !backInputDown) {
            power *= 0.96f
        }

        if (upInputDown && onGround() && energy > jumpEnergyCost && jumpCoolDown == 0 && engineInfo.canJump) {
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
            consumeEnergy(jumpEnergyCost)
            deltaMovement = deltaMovement.add(getUpVec(1f).scale(engineInfo.jumpForce))
            jumpCoolDown = engineInfo.jumpCoolDown
        }

        if (level() is ServerLevel) {
            consumeEnergy(energyCost)
        }

        val s0 = deltaMovement.dot(getViewVector(1f))
        leftWheelRot =
            (leftWheelRot - 1.25 * wheelRotSpeed * s0).toFloat() - 0.015f * wheelDifferential * Mth.clamp(
                0.4f * diffY,
                -5f,
                5f
            )
        rightWheelRot =
            (rightWheelRot - 1.25 * wheelRotSpeed * s0).toFloat() + 0.015f * wheelDifferential * Mth.clamp(
                0.4f * diffY,
                -5f,
                5f
            )

        if (isInFluidType || onGround()) {
            val water =
                (if (!isInFluidType && !onGround()) 0.05f else (if (isInFluidType && !onGround()) 0.3f else 1f)).toDouble()
            deltaMovement = deltaMovement.add(
                getViewVector(1f).scale(0.15 * water * targetSpeed * power)
            )
        }
    }

    @JvmStatic
    fun VehicleEntity.airShipEngine(engineInfo: EngineInfo.AirShip) {
        val buoyancy = engineInfo.buoyancy
        val energyCost = (engineInfo.energyCostRate * Mth.abs(power)).toInt()
        val maxForwardSpeedRate = engineInfo.maxForwardSpeedRate
        val maxBackwardSpeedRate = engineInfo.maxBackwardSpeedRate
        val maxUpSpeedRate = engineInfo.maxUpSpeedRate
        val maxDownSpeedRate = engineInfo.maxDownSpeedRate
        val powerAdd = engineInfo.increment * 0.05f
        val powerReduce = engineInfo.decrement * 0.05f
        val steeringSpeed = engineInfo.steeringSpeed * 0.05f
        val floatHeight = engineInfo.floatHeight
        val sprintMultiply = engineInfo.sprintMultiply

        if (buoyancy != 0.0) {
            val fluidFloat = buoyancy * VehicleVecUtils.getSubmergedHeight(this)
            deltaMovement = deltaMovement.add(0.0, fluidFloat, 0.0)
        }

        val hasPassenger = getPassengers().isNotEmpty()

        if (onGround()) {
            deltaMovement = deltaMovement.multiply(0.8, 1.0, 0.8)
        } else {
            val f = Mth.clamp(
                0.91499f - 0.01 * deltaMovement.lengthSqr() + 0.07 + 0.031f * Mth.abs(deltaMovement.normalize().dot(getViewVector(1f)).toFloat()), 0.01, 0.99
            ).toFloat()
            deltaMovement = deltaMovement.add(
                getViewVector(1f).scale(
                    (if (xRot < 0) -0.001 else (if (xRot > 0) 0.001 else 0.0)) * deltaMovement.length()
                )
            )
            deltaMovement = deltaMovement.multiply(f.toDouble(), 0.9, f.toDouble())
        }

        if (isInFluidType && tickCount % 4 == 0 && VehicleVecUtils.getSubmergedHeight(this) > 0.5 * bbHeight) {
            deltaMovement = deltaMovement.multiply(0.6, 0.6, 0.6)
        }

        if (level().isClientSide) {
            if (isInFluidType && deltaMovement.horizontalDistanceSqr() > 0.3162) {
                addRandomParticle(ParticleTypes.CLOUD, position().add(
                    0.0,
                    VehicleVecUtils.getSubmergedHeight(this) - 0.2,
                    0.0),
                    1f, level(), 0f, (2 + 4 * deltaMovement.length()).toInt())

                addRandomParticle(ParticleTypes.BUBBLE_COLUMN_UP, position().add(
                    0.0,
                    VehicleVecUtils.getSubmergedHeight(this) - 0.2,
                    0.0), 1f, level(), 0f, (2 + 10 * deltaMovement.length()).toInt())
            }
        }

        val passenger0 = getFirstPassenger()

        if (energy <= energyCost || (maxEnergy > 0 && energy <= 0)) {
            forwardInputDown = false
            backInputDown = false
            leftInputDown = false
            rightInputDown = false
            upInputDown = false
            downInputDown = false
            power *= 0.95f
        }

        if (passenger0 == null) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
            upInputDown = false
            downInputDown = false
            power = 0f
        }

        val maxPower = if (sprintInputDown) 1.25f else (if (power > 1) power - 0.002f else 1f)

        if (forwardInputDown && !backInputDown) {
            power = Math.min(power + (if (power < 0) powerAdd * 2f else powerAdd) * (maxPower - (Mth.abs(power) / 1.02f)), maxPower)
        }

        if (backInputDown) {
            power = Math.max(power - (if (power > 0) powerReduce * 4f else powerReduce) * (maxPower - (Mth.abs(power) / 1.02f)), -1f)
        }

        if (rightInputDown) {
            holdTick++
            deltaRot -= steeringSpeed * 0.1125f * Math.min(holdTick, 40)
        } else if (leftInputDown) {
            holdTick++
            deltaRot += steeringSpeed * 0.1125f * Math.min(holdTick, 40)
        } else {
            holdTick = 0
        }

        if (!forwardInputDown && !backInputDown) {
            power *= 0.96f
        }

        targetSpeed = if (power > 0) {
            (maxForwardSpeedRate).toDouble() * (if (sprintInputDown) sprintMultiply else 1.0)
        } else {
            (maxBackwardSpeedRate).toDouble()
        }

        if (upInputDown) {
            liftSpeed = Mth.clamp(liftSpeed + 0.05f, -1f, 1f)
        }

        if (downInputDown) {
            liftSpeed = Mth.clamp(liftSpeed - 0.05f, -1f, 1f)
        }

        if (health > 0) {
            xRot *= 0.97f
            roll *= 0.97f
        }

        if (health > 0) {
            if (!upInputDown && !downInputDown) {
                liftSpeed *= 0.8f
            }

            liftSpeed -= 0.01f * deltaMovement.y.toFloat()

            if (!hasPassenger) {
                val groundY = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, blockX, blockZ).toDouble()
                liftSpeed = if (groundY > level().minBuildHeight) {
                    val distToGround = y - groundY
                    val diff = (distToGround - floatHeight) * 0.05f
                    Math.clamp(liftSpeed - diff.toFloat(), -0.25f, 0.25f)
                } else {
                    Math.max(liftSpeed - 0.025f, -0.25f)
                }
            }
        } else {
            liftSpeed = Math.max(liftSpeed - 0.01f, -3f)
        }

        if (level() is ServerLevel) {
            consumeEnergy(energyCost)
        }

        deltaRot *= Math.max(0.8f - 0.01f * deltaMovement.horizontalDistance(), 0.3).toFloat()

        rudderRot = Mth.clamp(
            rudderRot + deltaRot,
            -1f,
            1f
        ) * 0.85f

        yRot = (yRot - (if (isInFluidType) 0.5 else 1.0) * deltaRot).toFloat()
        deltaMovement = deltaMovement.add(getViewVector(1f).scale(power * targetSpeed * 0.01))
        deltaMovement = if (liftSpeed >= 0) {
            deltaMovement.add(0.0, maxUpSpeedRate * 0.06 * liftSpeed, 0.0)
        } else {
            deltaMovement.add(0.0, maxDownSpeedRate * 0.06 * liftSpeed, 0.0)
        }
    }

    fun VehicleEntity.moveWithOutPower(player: Player, forward: Boolean) {
        deltaMovement = deltaMovement.add(getViewVector(1f).scale((if (forward) 0.1f else -0.1f).toDouble()))
        if (player is ServerPlayer) {
            player.level().playSound(null, player.onPos, SoundEvents.BOAT_PADDLE_LAND, SoundSource.PLAYERS, 1f, 1f)
        }
        player.causeFoodExhaustion(0.03f)

        forwardInputDown = false
        backInputDown = false
    }

    /**
     * 查找实体下方半球区域内最近的降落辅助方块位置
     *
     * @param radius 搜索半径
     * @return 辅助方块顶面位置，如果未找到则返回null
     */
    fun VehicleEntity.findNearestLandingPos(radius: Int): Vec3? {
        val world = level()
        val entityPos = blockPosition()
        val landingBlocks = ArrayList<BlockPos?>()

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
            position().distanceToSqr(pos!!.x + 0.5, (pos.y + 1).toDouble(), pos.z + 0.5)
        })

        return landingBlocks[0]?.center
    }

    fun VehicleEntity.updateAutoLanding(landingTarget: Vec3) {
        // 计算水平方向上的偏移向量 (忽略Y轴)
        val currentPos = position()
        val horizontalOffset = Vec3(
            landingTarget.x - currentPos.x,
            0.0,
            landingTarget.z - currentPos.z
        )

        deltaMovement = deltaMovement.multiply(0.975, 0.99, 0.975)

        // 计算距离和方向
        val horizontalDistance = horizontalOffset.length()
        val horizontalDirection = if (horizontalDistance > 0) horizontalOffset.normalize() else Vec3.ZERO


        // 倾斜平滑因子
        val tiltSmoothingFactor = 0.1f

        val horizontalDistanceNew = horizontalDistance - 5 * deltaMovement.horizontalDistance()

        // 计算需要的倾斜角度 (与距离成正比，但有最大限制)
        // 直升机辅助降落这一块
        // 最大倾斜角度(度)
        val maxTiltAngle = 15.0f
        val targetTilt = Math.min(maxTiltAngle.toDouble(), horizontalDistanceNew * 2).toFloat()

        // 将世界方向转换为本地倾斜方向
        // 需要考虑直升机的当前偏航角(yRot)
        val yawRad = Math.toRadians(-yRot)
        val localDirection = Vec3(
            horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
            0.0,
            horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        )

        // 计算目标俯仰和滚转
        val targetXRot = (-localDirection.z * targetTilt).toFloat()
        val targetZRot = (localDirection.x * targetTilt).toFloat()

        // 平滑过渡到目标姿态
        xRot = lerpAngle(xRot, -targetXRot, tiltSmoothingFactor)
        setZRot(lerpAngle(roll, -targetZRot, tiltSmoothingFactor))
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

    /**
     * 固定翼飞机自动盘旋飞控（左舷朝向圆心）
     *
     * 两阶段混合制导：
     * - **切线飞行**：左舷朝向圆心的切线方向，维持绕圈
     * - **径向修正**：圈内时朝圈外飞，圈外时朝圆心飞
     *
     * 混合权重随径向距离自适应——越接近轨道越倾向切线，越远离越倾向径向修正。
     * 径向混合在极近轨道(<5%半径≈50m)平滑退场→纯P修正防振荡，
     * 其余距离全效混合。额外 P=0.12, clamp ±35°。
     *
     * 应在 [VehicleEntity.baseTick] 中 engine work 之后调用，
     * 仅对 [EngineType.AIRCRAFT] 类型生效。
     */
    @JvmStatic
    fun VehicleEntity.aircraftLoiter() {
        // ========== 0. 解析四元数盘旋参数 ==========
        // x=centerX, y=centerY(高度), z=centerZ, w=radius

        val lp = loiterParams
        val centerX = lp.x().toDouble()
        val altitude = lp.y().toDouble()
        val centerZ = lp.z().toDouble()
        val radius = lp.w().toDouble()

        // ========== 1. 位置与误差计算 ==========

        val dx = x - centerX
        val dz = z - centerZ
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val radialError = horizontalDist - radius

        // ========== 2. 两阶段航向制导 ==========

        // 航向 A：左舷朝向圆心的切线方向（盘旋阶段）
        val tangentYaw = Math.toDegrees(atan2(-dz, -dx))
        val errorTangent = Mth.wrapDegrees(tangentYaw - yRot.toDouble())

        // 航向 B：指向圆心（拦截阶段），圈内时反转180°指向圈外
        val toCenterYaw = Math.toDegrees(atan2(dx, -dz))
        val errorToCenter = Mth.wrapDegrees(toCenterYaw - yRot.toDouble())
        val errorOutward = Mth.wrapDegrees(toCenterYaw + 180.0 - yRot.toDouble())
        val radialYaw = if (radialError < 0) errorOutward else errorToCenter

        // 混合权重：越远越倾向径向飞行，越近越倾向切线飞行
        // 仅在极近轨道（20m内）平滑退场，避免方向翻转导致振荡
        val distFromOrbit = abs(radialError)
        val blend = Mth.clamp(1.0 - distFromOrbit / radius, 0.0, 0.8)
        val blendZone = 20.0
        val blendFactor = Mth.clamp((distFromOrbit - blendZone) / blendZone, 0.0, 1.0)
        val effectiveRadial = (1.0 - blend) * blendFactor
        var yawError = (errorTangent * (1.0 - effectiveRadial) + radialYaw * effectiveRadial).toFloat()

        // ========== 3. 径向位置精修 ==========

        yawError += Mth.clamp(radialError.toFloat() * 0.12f, -35f, 35f)

        // ========== 4. 偏航控制 ==========

        mouseMoveSpeedX = Mth.clamp(yawError, -20f, 20f)
        deltaRot += yawError * -0.01f * Mth.clamp(1 - (Mth.abs(roll) / 30), 0f, 1f)

        // ========== 5. 高度控制 ==========

        val altError = altitude - y
        val targetPitch = Mth.clamp(altError.toFloat() * -0.15f, -10f, 10f)
        xRot = Mth.lerp(0.01f, xRot, targetPitch)
        mouseMoveSpeedY = Mth.clamp(altError.toFloat() * -0.02f, -1f, 1f)

        // ========== 6. 油门控制（高度自适应防失速） ==========

        // 低于目标高度 → 爬升需增大油门防失速；高于目标高度 → 缓慢减油
        val powerTarget = if (altError > 0) {
            Mth.clamp(0.9f + altError.toFloat() * 0.002f, 0.9f, 2.0f)
        } else {
            Mth.clamp(0.9f + altError.toFloat() * 0.0005f, 0.5f, 0.9f)
        }
        power = Mth.lerp(0.05f, power, powerTarget)

        // ========== 7. 障碍物规避 ==========
        // 通过 mouseMoveSpeedY 模拟鼠标输入，让 aircraftEngine() 的气动模型
        // 自然限制机动性能。功率被动根据爬升角度缓慢设下限，防失速不主动拉。
        // 返回 null 表示无威胁或已禁用，跳过整段规避逻辑。

        obstacleAvoidanceOverride()?.let { avoidance ->
            if (avoidance.mouseYBlendFactor > 0f) {
                mouseMoveSpeedY = Mth.lerp(avoidance.mouseYBlendFactor, mouseMoveSpeedY, avoidance.mouseYOverride)

                // 被动功率托底：爬升越陡下限越高，只托不拉，变化极慢(lerp 0.02)
                val climbDemand = Mth.clamp(-avoidance.mouseYOverride / 22f, 0f, 1f)
                val minPower = Mth.lerp(climbDemand, 1.1f, 1.8f)
                if (power < minPower) {
                    power = Mth.lerp(0.02f, power, minPower)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  障碍物规避系统
    // ═══════════════════════════════════════════════════════════════

    // ─── 常量 ───

    /** 扫描间隔 (tick) — 4 tick = 0.2s，保证响应速度 */
    private const val OA_SCAN_INTERVAL = 5

    /** 前方扫描最大距离 (格) */
    private const val OA_SCAN_RANGE = 360.0

    /** 扫描步长 (格) */
    private const val OA_SCAN_STEP = 20.0

    /** 扇形扫描半角 (度) */
    private const val OA_FAN_HALF_ANGLE = 15.0

    /** 扇形扫描射线数 (必须为奇数，中间=正前方) */
    private const val OA_FAN_RAYS = 9

    /** 基础安全余量 (格) — 飞机需要保持高于地形的最小高度 */
    private const val OA_SAFETY_BASE = 10.0

    /** 速度加成安全余量系数 — 速度越快越早反应 */
    private const val OA_SAFETY_SPEED_FACTOR = 0.15

    /** 爬升紧迫性特征距离 (格) — urgency = D/(D+d)，D越大越早开始反应 */
    private const val OA_DANGER_DISTANCE = 300.0

    /** 最大爬升俯仰角 (度, 抬头为负) — 用于 estimateMaxClimb 爬升能力估算 */
    private const val OA_MAX_CLIMB_PITCH = -25f

    /** 跳过低速扫描阈值 (m/s 真实秒速) */
    private const val OA_SKIP_SPEED = 5.0

    /** 释放宽限期 (tick) — 威胁消失后持续响应N tick防振荡 */
    private const val OA_RELEASE_GRACE = 12

    // ─── 数据类 ───

    /** 单条射线的扫描结果 */
    private data class RayScan(
        val angleOffset: Double,            // 射线偏移角度 (弧度)
        val firstImpactDist: Double,        // 首次碰撞距离，-1 表示畅通
        val firstImpactHeight: Double       // 首次碰撞点的地形高度
    )

    /** 前方扫描总结果 */
    private data class ObstacleScan(
        val hasThreat: Boolean,
        val threatLevel: Float,             // 0~1，越大越紧急
        val closestDist: Double,            // 最近碰撞距离，-1 表示无威胁
        val impactX: Double,                // 碰撞点 X
        val impactZ: Double,                // 碰撞点 Z
        val obstacleRelHeight: Double,      // 障碍物高于飞行器的高度
        val rays: List<RayScan>
    ) {
        companion object {
            val CLEAR = ObstacleScan(false, 0f, -1.0, 0.0, 0.0, 0.0, emptyList())
        }
    }

    /** 规避策略 */
    private enum class AvoidanceStrategy { NONE, CLIMB, EMERGENCY_CLIMB }

    /**
     * 规避输出：通过 mouseMoveSpeedY 模拟鼠标输入，
     * 由 aircraftEngine() 的气动模型（pitchSpeed、clampPitch、襟翼等）自然处理。
     * 功率由集成层根据爬升角度被动托底，不在此处主动设置。
     */
    private data class AvoidanceOutput(
        val mouseYOverride: Float,        // mouseMoveSpeedY 覆盖值（负=抬头）
        val mouseYBlendFactor: Float      // 俯仰混合因子 0~1
    ) {
        companion object {
            val NONE = AvoidanceOutput(0f, 0f)
        }
    }

    // ─── 缓存（按实体 ID 索引，防止多架飞行器共享状态） ───

    private data class AvoidanceCache(
        val scan: ObstacleScan = ObstacleScan.CLEAR,
        val threatActive: Boolean = false,
        val strategy: AvoidanceStrategy = AvoidanceStrategy.NONE,
        val lastScanTick: Int = -1,
        val releaseGrace: Int = 0     // >0 表示威胁已消失但仍在宽限期内
    )

    private val avoidanceCache = mutableMapOf<Int, AvoidanceCache>()

    /** 清理长时间未使用的缓存条目（每 200 tick 触发一次） */
    private fun cleanupAvoidanceCache(currentTick: Int) {
        if (currentTick % 200 != 0) return
        val threshold = currentTick - 400
        avoidanceCache.entries.removeAll { (_, v) -> v.lastScanTick < threshold }
    }

    // ─── 主入口 ───

    /**
     * 障碍物规避主逻辑，在 [aircraftLoiter] 末尾调用。
     * 扫描帧执行完整分析，非扫描帧用当前位置实时更新距离后继续响应。
     *
     * @return 规避输出；null 表示无威胁（缓存已清），调用方可跳过整段规避代码
     */
    private fun VehicleEntity.obstacleAvoidanceOverride(): AvoidanceOutput? {
        // 建筑高度以上：不可能有地形，直接禁用
        if (y > level().maxBuildHeight) {
            avoidanceCache.remove(id)
            return null
        }

        cleanupAvoidanceCache(tickCount)
        val cache = avoidanceCache[id]

        // 非扫描帧：用缓存数据 + 实时距离
        if (tickCount % OA_SCAN_INTERVAL != 0) {
            if (cache == null) return null

            // 宽限期倒计时：威胁已消失，逐 tick 衰减后释放
            if (!cache.threatActive) {
                if (cache.releaseGrace > 1) {
                    avoidanceCache[id] = cache.copy(releaseGrace = cache.releaseGrace - 1)
                    val decay = cache.releaseGrace.toFloat() / OA_RELEASE_GRACE
                    val output = computeAvoidance(cache.strategy, cache.scan, cache.scan.closestDist)
                    return output.copy(
                        mouseYBlendFactor = output.mouseYBlendFactor * decay,
                        mouseYOverride = output.mouseYOverride * decay
                    )
                }
                avoidanceCache.remove(id)
                return null
            }

            // 威胁活跃中：计算实时距离
            val dx = x - cache.scan.impactX
            val dz = z - cache.scan.impactZ
            val currentDist = sqrt(dx * dx + dz * dz)
            // 已经飞过碰撞点或距离增加 → 威胁解除，进入宽限期
            if (currentDist > cache.scan.closestDist * 1.5 || currentDist < 0) {
                avoidanceCache[id] = cache.copy(threatActive = false, releaseGrace = OA_RELEASE_GRACE)
                return computeAvoidance(cache.strategy, cache.scan, currentDist)
            }
            val output = computeAvoidance(cache.strategy, cache.scan, currentDist)
            // 实时距离计算后威胁低于阈值 → 进入宽限期
            if (output.mouseYBlendFactor <= 0f) {
                avoidanceCache[id] = cache.copy(threatActive = false, releaseGrace = OA_RELEASE_GRACE)
                return output
            }
            return output
        }

        // 低速门控
        val speedPerTick = deltaMovement.length()
        val speedMs = speedPerTick * 20.0
        if (speedMs < OA_SKIP_SPEED) {
            avoidanceCache.remove(id)
            return null
        }

        // 1. 前向扫描
        val scan = scanTerrainAhead(speedMs)
        if (!scan.hasThreat) {
            // 无威胁：若之前有活跃威胁则进入宽限期防振荡，否则直接清
            if (cache != null && cache.threatActive) {
                avoidanceCache[id] = cache.copy(threatActive = false, releaseGrace = OA_RELEASE_GRACE)
                val output = computeAvoidance(cache.strategy, cache.scan, cache.scan.closestDist)
                return output.copy(
                    mouseYBlendFactor = output.mouseYBlendFactor * 0.5f,
                    mouseYOverride = output.mouseYOverride * 0.5f
                )
            }
            avoidanceCache.remove(id)
            return null
        }

        // 2. 决策
        val strategy = decideStrategy(scan.obstacleRelHeight, scan.closestDist)

        // 3. 计算规避输出
        val output = computeAvoidance(strategy, scan, scan.closestDist)

        avoidanceCache[id] = AvoidanceCache(
            scan = scan,
            threatActive = true,
            strategy = strategy,
            lastScanTick = tickCount
        )

        return output
    }

    // ─── 1. 前向扇形扫描 ───

    /**
     * 沿速度方向发射 OA_FAN_RAYS 条射线，每条射线步进 OA_SCAN_STEP 格，
     * 使用 MOTION_BLOCKING 高度图检测地形碰撞。
     */
    private fun VehicleEntity.scanTerrainAhead(speed: Double): ObstacleScan {
        val vel = deltaMovement
        val forward = Vec3(vel.x, 0.0, vel.z).normalize()
        val level = level()
        val maxSteps = (OA_SCAN_RANGE / OA_SCAN_STEP).toInt()

        val angles = computeFanAngles()
        val rays = mutableListOf<RayScan>()
        var closestDist = Double.MAX_VALUE
        var impactX = 0.0
        var impactZ = 0.0
        var obstacleRelHeight = 0.0
        val safetyMargin = OA_SAFETY_BASE + speed * OA_SAFETY_SPEED_FACTOR

        for (angleRad in angles) {
            val rayDir = forward.yRot(angleRad.toFloat())
            var firstHit = -1.0
            var firstHeight = 0.0
            var rayBlocked = false

            for (step in 1..maxSteps) {
                val dist = step * OA_SCAN_STEP
                val sx = x + rayDir.x * dist
                val sz = z + rayDir.z * dist
                val cx = sx.toInt() shr 4
                val cz = sz.toInt() shr 4

                if (!level.hasChunk(cx, cz)) {
                    // 未加载区块：静默截断射线，不标记威胁。
                    // 扫描范围 300m 通常超出服务器视距，若视为阻塞则每条射线都会产生假威胁。
                    // 真正的危险地形会在区块随飞机接近而加载后被下轮扫描捕获。
                    rayBlocked = true
                    break
                }

                val terrainY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sx.toInt(), sz.toInt()).toDouble()
                val dangerAlt = terrainY + safetyMargin

                if (y < dangerAlt) {
                    firstHit = dist
                    firstHeight = terrainY
                    rayBlocked = true
                    break
                }
            }

            if (!rayBlocked) {
                rays.add(RayScan(angleRad, -1.0, 0.0))
            } else {
                rays.add(RayScan(angleRad, firstHit, firstHeight))
                if (firstHit < closestDist) {
                    closestDist = firstHit
                    val dist = firstHit
                    impactX = x + rayDir.x * dist
                    impactZ = z + rayDir.z * dist
                    obstacleRelHeight = firstHeight + safetyMargin - y
                }
            }
        }

        if (closestDist >= OA_SCAN_RANGE) {
            return ObstacleScan.CLEAR
        }

        // 二次威胁曲线：远距离增长缓慢，近距离快速攀升
        val t = Mth.clamp(1.0 - closestDist / OA_SCAN_RANGE, 0.0, 1.0)
        val threatLevel = (t * t).toFloat()

        return ObstacleScan(
            hasThreat = true,
            threatLevel = threatLevel,
            closestDist = closestDist,
            impactX = impactX,
            impactZ = impactZ,
            obstacleRelHeight = obstacleRelHeight,
            rays = rays
        )
    }

    /** 计算扇形扫描各射线的偏转角 (弧度) */
    private fun computeFanAngles(): List<Double> {
        val rays = OA_FAN_RAYS
        if (rays <= 1) return listOf(0.0)
        val halfDeg = OA_FAN_HALF_ANGLE
        val stepDeg = (2.0 * halfDeg) / (rays - 1)
        return (0 until rays).map { i ->
            Math.toRadians(-halfDeg + i * stepDeg)
        }
    }

    // ─── 2. 爬升能力估算 ───

    /**
     * 估算在 [distance] 格内飞行器能爬升的最大高度。
     * 受限于气动参数和当前速度。
     */
    private fun VehicleEntity.estimateMaxClimb(distance: Double): Double {
        val speedPerTick = deltaMovement.length()
        val speedMs = speedPerTick * 20.0  // 换算真实秒速
        if (speedMs < 1.0) return 10.0

        val lift = (engineInfo as? EngineInfo.Aircraft)?.liftSpeed ?: 1f
        // 气动爬升角 ≈ atan(speedMs * 0.008 * lift * 4.0)，不超过 MAX_CLIMB_PITCH (25°)
        val climbSlope = min(
            tan(Math.toRadians((-OA_MAX_CLIMB_PITCH).toDouble())),
            speedMs * 0.008 * lift.toDouble() * 4.0
        )
        return distance * climbSlope
    }

    // ─── 3. 策略决策 ───

    private fun VehicleEntity.decideStrategy(
        obstacleRelHeight: Double,
        distance: Double
    ): AvoidanceStrategy {
        if (obstacleRelHeight <= 0) return AvoidanceStrategy.NONE
        return if (obstacleRelHeight < estimateMaxClimb(distance)) AvoidanceStrategy.CLIMB
               else AvoidanceStrategy.EMERGENCY_CLIMB
    }

    // ─── 4. 规避输出计算 ───

    /**
     * @param currentDist 飞机到碰撞点的当前实时距离
     *
     * 通过 mouseMoveSpeedY 模拟鼠标输入，aircraftEngine() 的气动模型
     * 自然限制 pitchSpeed/clampPitch/襟翼等，确保机动不超性能边界。
     */
    private fun computeAvoidance(
        strategy: AvoidanceStrategy,
        scan: ObstacleScan,
        currentDist: Double
    ): AvoidanceOutput {
        if (strategy == AvoidanceStrategy.NONE) return AvoidanceOutput.NONE

        val distClamped = Mth.clamp(currentDist, 0.0, OA_SCAN_RANGE)

        // sqrt 曲线：远距离就有明显反应，中距离快速攀升
        val t = Mth.clamp(1.0 - distClamped / OA_SCAN_RANGE, 0.0, 1.0)
        val liveThreat = Math.sqrt(t).toFloat()
        if (liveThreat <= 0.02f) return AvoidanceOutput.NONE

        // urgency = D/(D+d)，D=200
        val urgency = (OA_DANGER_DISTANCE / (OA_DANGER_DISTANCE + distClamped)).toFloat()
        val effectiveFactor = liveThreat * urgency

        return when (strategy) {
            AvoidanceStrategy.CLIMB -> {
                val mouseYTarget = Mth.clamp(-30f * effectiveFactor, -22f, -5f)
                val mouseYBlend = Mth.clamp(effectiveFactor * 2.5f, 0f, 1f)
                AvoidanceOutput(mouseYOverride = mouseYTarget, mouseYBlendFactor = mouseYBlend)
            }

            AvoidanceStrategy.EMERGENCY_CLIMB -> AvoidanceOutput(
                mouseYOverride = -22f, mouseYBlendFactor = 1f
            )

            AvoidanceStrategy.NONE -> AvoidanceOutput.NONE
        }
    }

    // ─── 工具 ───
}
