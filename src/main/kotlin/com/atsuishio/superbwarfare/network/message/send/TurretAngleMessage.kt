package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class TurretAngleMessage(val yRot: Float, val xRot: Float) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val entity = player.vehicle

        if (entity is VehicleEntity &&
            entity.usesDirectTurretControl() &&
            entity.turretControllerIndex == entity.getSeatIndex(player)
        ) {
            entity.applyDirectTurretAngles(yRot, xRot)
        }
    }
}
