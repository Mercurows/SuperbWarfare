package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.physics.*
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedRenderState

@RegisterPacket
@Serializable
data class VehiclePhysicsStateMessage(val id: Int, val frame: Long, val sequence: Long,
    val values: List<Double>, val grounded: Boolean,
    val leftSuspension: Double = 0.0, val rightSuspension: Double = 0.0,
    val leftTrackPhase: Double = 0.0, val rightTrackPhase: Double = 0.0,
    /** Flattened wheel records: index, side, suspension, steering, spin, grounded, steerable. */
    val wheelValues: List<Double> = emptyList(),
    val tracked: TrackedRenderState? = null) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        if (values.size != 13 || values.any { !it.isFinite() } ||
            !leftSuspension.isFinite() || !rightSuspension.isFinite() ||
            !leftTrackPhase.isFinite() || !rightTrackPhase.isFinite() ||
            wheelValues.size % 7 != 0 || wheelValues.any { !it.isFinite() } ||
            tracked?.finite() == false) return
        val entity = clientLevel?.getEntity(id) ?: return
        val q = Quaternionf(values[3].toFloat(), values[4].toFloat(), values[5].toFloat(), values[6].toFloat())
        if (!q.isFinite || q.lengthSquared() < .001) return
        VehiclePhysics.accept(entity, RigidState(frame, sequence, Vec3(values[0], values[1], values[2]), q.normalize(),
            Vec3(values[7],values[8],values[9]), Vec3(values[10],values[11],values[12]), grounded),
            VehiclePhysicsRenderState(frame,
                leftTravel = leftSuspension.coerceIn(-2.0, 2.0).toFloat(),
                rightTravel = rightSuspension.coerceIn(-2.0, 2.0).toFloat(),
                leftTrackPhase = leftTrackPhase.toFloat(),
                rightTrackPhase = rightTrackPhase.toFloat(),
                wheels = wheelValues.chunked(7).mapNotNull { values ->
                    val side = when (values[1].toInt()) {
                        0 -> WheelSide.LEFT
                        1 -> WheelSide.RIGHT
                        else -> return@mapNotNull null
                    }
                    WheelRenderState(
                        index = values[0].toInt(),
                        side = side,
                        suspensionTravel = values[2].coerceIn(-2.0, 2.0).toFloat(),
                        steeringAngle = values[3].coerceIn(-Math.PI, Math.PI).toFloat(),
                        spin = values[4].toFloat(),
                        grounded = values[5] > .5,
                        steerable = values[6] > .5,
                    )
                },
                grounded = grounded,
                tracked = tracked))
    }
    companion object {
        internal fun of(id: Int, s: RigidState, render: VehiclePhysicsRenderState = VehiclePhysicsRenderState()) = VehiclePhysicsStateMessage(id, s.frame, s.sequence,
            listOf(s.position.x,s.position.y,s.position.z,s.rotation.x.toDouble(),s.rotation.y.toDouble(),s.rotation.z.toDouble(),s.rotation.w.toDouble(),
                s.velocity.x,s.velocity.y,s.velocity.z,s.angularVelocity.x,s.angularVelocity.y,s.angularVelocity.z), s.grounded,
            render.leftTravel.toDouble(), render.rightTravel.toDouble(),
            render.leftTrackPhase.toDouble(), render.rightTrackPhase.toDouble(),
            render.wheels.flatMap { wheel ->
                listOf(
                    wheel.index.toDouble(),
                    if (wheel.side == WheelSide.LEFT) 0.0 else 1.0,
                    wheel.suspensionTravel.toDouble(),
                    wheel.steeringAngle.toDouble(),
                    wheel.spin.toDouble(),
                    if (wheel.grounded) 1.0 else 0.0,
                    if (wheel.steerable) 1.0 else 0.0,
                )
            },
            render.tracked)
    }
}
