package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.DistantVehicleManager
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class VehicleSnapshot(
    val entityId: Int,
    val type: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val turretYRot: Float,
    val turretXRot: Float,
    val skinId: String,
)

@Serializable
data class DistantVehiclesMessage(
    val interval: Int,
    val vehicles: List<VehicleSnapshot>,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        DistantVehicleManager.handleMessage(this@DistantVehiclesMessage)
    }
}
