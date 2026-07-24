package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.getValue
import com.atsuishio.superbwarfare.entity.setValue
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.VectorTool
import com.mojang.math.Axis
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Quaterniond
import java.util.function.Function
import kotlin.math.cos
import kotlin.math.sin

class UralEntity(type: EntityType<UralEntity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {

    private var leftDoorOpen = false
    private var rightDoorOpen = false
    private var lastZadOpen = false

    // Hinge angle (degrees) driving the "zad" tailgate's collision box; eased toward
    // ZAD_MAX_ANGLE/0 in updateOBB() at ZAD_ROT_STEP degrees/tick, tuned to match the real
    // 1-second duration of animation.zad.open/close in ural.animation.json (see ZAD_ROT_STEP).
    private var zadRot = 0f

    var zadOpen by ZAD_OPEN

    init {
        positionTransform["ZadHinge"] = Function { partialTicks -> getZadTransform(partialTicks) }
        rotationTransform["ZadHinge"] = Function { partialTicks -> getZadRotation(partialTicks) }

        positionTransform["WheelTurnL"] =
            Function { partialTicks -> getWheelTurnTransform(FRONT_WHEEL_L_PIVOT, leftWheelRot, partialTicks) }
        positionTransform["WheelTurnR"] =
            Function { partialTicks -> getWheelTurnTransform(FRONT_WHEEL_R_PIVOT, rightWheelRot, partialTicks) }
        rotationTransform["WheelTurnL"] = Function { partialTicks -> getWheelTurnRotation(leftWheelRot, partialTicks) }
        rotationTransform["WheelTurnR"] = Function { partialTicks -> getWheelTurnRotation(rightWheelRot, partialTicks) }

        // Center and rear axles don't steer, only roll.
        positionTransform["WheelRollCenterL"] =
            Function { partialTicks -> getWheelRollTransform(CENTER_WHEEL_L_PIVOT, leftWheelRot, partialTicks) }
        positionTransform["WheelRollCenterR"] =
            Function { partialTicks -> getWheelRollTransform(CENTER_WHEEL_R_PIVOT, rightWheelRot, partialTicks) }
        positionTransform["WheelRollRearL"] =
            Function { partialTicks -> getWheelRollTransform(REAR_WHEEL_L_PIVOT, leftWheelRot, partialTicks) }
        positionTransform["WheelRollRearR"] =
            Function { partialTicks -> getWheelRollTransform(REAR_WHEEL_R_PIVOT, rightWheelRot, partialTicks) }
        rotationTransform["WheelRollCenterL"] = Function { partialTicks -> getWheelRollRotation(leftWheelRot, partialTicks) }
        rotationTransform["WheelRollCenterR"] = Function { partialTicks -> getWheelRollRotation(rightWheelRot, partialTicks) }
        rotationTransform["WheelRollRearL"] = Function { partialTicks -> getWheelRollRotation(leftWheelRot, partialTicks) }
        rotationTransform["WheelRollRearR"] = Function { partialTicks -> getWheelRollRotation(rightWheelRot, partialTicks) }
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(ZAD_OPEN, false)
    }

    // Right-clicking the tailgate (the rear ~1.7 blocks of the cargo bed) toggles it,
    // instead of falling through to the normal mount/menu interaction.
    override fun interactAt(pPlayer: Player, pVec: Vec3, pHand: InteractionHand): InteractionResult {
        if (!this.isWreck) {
            val yawRad = Math.toRadians(this.yRot.toDouble())
            val localForward = -pVec.x * sin(yawRad) + pVec.z * cos(yawRad)
            if (localForward < -3.3) {
                if (!level().isClientSide) {
                    zadOpen = !zadOpen
                }
                return InteractionResult.sidedSuccess(level().isClientSide)
            }
        }
        return super.interactAt(pPlayer, pVec, pHand)
    }

    override fun updateOBB() {
        val target = if (zadOpen) ZAD_MAX_ANGLE else 0f
        zadRot = if (zadRot < target) {
            (zadRot + ZAD_ROT_STEP).coerceAtMost(target)
        } else {
            (zadRot - ZAD_ROT_STEP).coerceAtLeast(target)
        }
        super.updateOBB()
    }

    // Hinge pivot of the "zad" bone (bottom edge of the tailgate), converted from the geo
    // model's pivot [2, 30, 78.5] (16 units/block, Z axis flipped relative to vehicle space).
    private fun getZadTransform(partialTicks: Float): Matrix4d {
        val transform = getVehicleTransform(partialTicks)
        transform.translate(0.125, 1.875, -4.90625)
        transform.rotate(Axis.XP.rotationDegrees(zadRot))
        return transform
    }

    private fun getZadRotation(partialTicks: Float): Quaterniond {
        val doorRot = Axis.XP.rotationDegrees(zadRot)
        return VectorTool.combineRotations(partialTicks, this).mul(Quaterniond(doorRot))
    }

    // Front-axle steering pivot (wheelL0Turn/wheelR0Turn's bone pivot, converted from geo
    // units [21/-17, 11.99792, -47]); shares rudderRot with the visual steering bones, and
    // rolls opposite to SbmVehicleRenderer's 1.5f*wheelRot (confirmed by testing that the
    // renderer's sign made the hitbox spin backward relative to the truck's actual travel).
    private fun getWheelTurnTransform(pivot: Vec3, wheelRot: Float, partialTicks: Float): Matrix4d {
        val transform = getVehicleTransform(partialTicks)
        transform.translate(pivot.x, pivot.y, pivot.z)
        transform.rotate(Axis.YP.rotation(rudderRot))
        transform.rotate(Axis.XP.rotation(-1.5f * wheelRot))
        return transform
    }

    private fun getWheelTurnRotation(wheelRot: Float, partialTicks: Float): Quaterniond {
        val yawRot = Axis.YP.rotation(rudderRot)
        val pitchRot = Axis.XP.rotation(-1.5f * wheelRot)
        return VectorTool.combineRotations(partialTicks, this).mul(Quaterniond(yawRot)).mul(Quaterniond(pitchRot))
    }

    // Center/rear axle pivots (fixed, no steering) - same roll as the front wheels.
    private fun getWheelRollTransform(pivot: Vec3, wheelRot: Float, partialTicks: Float): Matrix4d {
        val transform = getVehicleTransform(partialTicks)
        transform.translate(pivot.x, pivot.y, pivot.z)
        transform.rotate(Axis.XP.rotation(-1.5f * wheelRot))
        return transform
    }

    private fun getWheelRollRotation(wheelRot: Float, partialTicks: Float): Quaterniond {
        val pitchRot = Axis.XP.rotation(-1.5f * wheelRot)
        return VectorTool.combineRotations(partialTicks, this).mul(Quaterniond(pitchRot))
    }

    override fun baseTick() {
        super.baseTick()

        if (decoyInputDown) {
            horn()
        }

        if (!level().isClientSide) return
        val ctx = anim?.context ?: return

        // The door swing is a single open-then-close animation; play it once on every
        // mount AND dismount edge, rather than holding the door open for the whole ride.
        val driverPresent = getNthEntity(0) != null
        if (driverPresent != leftDoorOpen) {
            ctx.playAnimation("animation.door_l.open", AnimationPlayType.PLAY_ONCE_STOP)
        }
        leftDoorOpen = driverPresent

        // Seats 1 (middle) and 2 (right) are both boarded through the right door on the bench seat.
        val passengerPresent = getNthEntity(1) != null || getNthEntity(2) != null
        if (passengerPresent != rightDoorOpen) {
            ctx.playAnimation("animation.door_r.open", AnimationPlayType.PLAY_ONCE_STOP)
        }
        rightDoorOpen = passengerPresent

        // The tailgate is toggled (held open/closed) rather than auto-closing.
        if (zadOpen != lastZadOpen) {
            ctx.playAnimation(
                if (zadOpen) "animation.zad.open" else "animation.zad.close",
                AnimationPlayType.PLAY_ONCE_HOLD
            )
        }
        lastZadOpen = zadOpen
    }

    companion object {
        private const val ZAD_MAX_ANGLE = -100f
        // Tuned by testing: the tick-based hitbox ran ~2x faster than the door's real
        // 1-second animation, so this is halved from the naive 20-tick (5 deg/tick) value.
        private const val ZAD_ROT_STEP = 2.5f

        private val FRONT_WHEEL_L_PIVOT = Vec3(1.3125, 0.8099, 2.9375)
        private val FRONT_WHEEL_R_PIVOT = Vec3(-1.0625, 0.8099, 2.9375)
        private val CENTER_WHEEL_L_PIVOT = Vec3(1.3125, 0.8099, -1.5)
        private val CENTER_WHEEL_R_PIVOT = Vec3(-1.0625, 0.8099, -1.5)
        private val REAR_WHEEL_L_PIVOT = Vec3(1.3125, 0.8099, -3.25)
        private val REAR_WHEEL_R_PIVOT = Vec3(-1.0625, 0.8099, -3.25)

        val ZAD_OPEN: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(UralEntity::class.java, EntityDataSerializers.BOOLEAN)
    }
}
