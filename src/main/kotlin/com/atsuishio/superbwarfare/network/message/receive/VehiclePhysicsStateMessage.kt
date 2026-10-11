package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.physics.*
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

@RegisterPacket
@Serializable
data class VehiclePhysicsStateMessage(val id: Int, val frame: Long, val sequence: Long,
    val values: List<Double>, val grounded: Boolean,
    val leftSuspension: Double = 0.0, val rightSuspension: Double = 0.0,
    val leftTrackPhase: Double = 0.0, val rightTrackPhase: Double = 0.0) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        if (values.size != 13 || values.any { !it.isFinite() } ||
            !leftSuspension.isFinite() || !rightSuspension.isFinite() ||
            !leftTrackPhase.isFinite() || !rightTrackPhase.isFinite()) return
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
                grounded = grounded))
    }
    companion object {
        internal fun of(id: Int, s: RigidState, render: VehiclePhysicsRenderState = VehiclePhysicsRenderState()) = VehiclePhysicsStateMessage(id, s.frame, s.sequence,
            listOf(s.position.x,s.position.y,s.position.z,s.rotation.x.toDouble(),s.rotation.y.toDouble(),s.rotation.z.toDouble(),s.rotation.w.toDouble(),
                s.velocity.x,s.velocity.y,s.velocity.z,s.angularVelocity.x,s.angularVelocity.y,s.angularVelocity.z), s.grounded,
            render.leftTravel.toDouble(), render.rightTravel.toDouble(),
            render.leftTrackPhase.toDouble(), render.rightTrackPhase.toDouble())
    }
}
