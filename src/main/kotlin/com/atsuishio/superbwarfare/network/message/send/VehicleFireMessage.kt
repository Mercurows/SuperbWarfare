package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
data class VehicleFireMessage(
    val uuid: SerializedUUID?,
    val targetPos: SerializedVector3f?,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return

        if (targetPos != null) {
            vehicle.vehicleShoot(player, uuid, Vec3(targetPos))
        } else {
            vehicle.vehicleShoot(player, uuid, null)
        }
    }
}
