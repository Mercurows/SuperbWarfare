package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkin
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class SetVehicleSkinMessage(val entityId: Int, val skinId: String) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val vehicle = sender().level().getEntity(entityId) as? VehicleEntity ?: return
        // Validate: blank = vanilla, otherwise skin must exist in VehicleSkin
        if (skinId.isBlank() || VehicleSkin.getSkins(vehicle.type).skins.any { it.id == skinId }) {
            vehicle.skinId = skinId
        }
    }
}
