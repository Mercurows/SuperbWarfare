package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class TurretAngleMessage(
    /** 是否处于“直接控制”模式（第一人称/瞄准）；第三人称为 false，回归旧视线追随 */
    val active: Boolean,
    val yRot: Float,
    val xRot: Float
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val entity = player.vehicle

        if (entity is VehicleEntity &&
            entity.hasTurret() &&
            entity.turretControllerIndex == entity.getSeatIndex(player)
        ) {
            entity.applyDirectTurretRequest(active, yRot, xRot)
        }
    }
}
