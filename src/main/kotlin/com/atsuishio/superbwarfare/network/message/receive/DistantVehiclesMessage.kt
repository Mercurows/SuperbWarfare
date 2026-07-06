package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.DistantVehicleManager
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import kotlinx.serialization.Serializable

@Serializable
data class VehicleSnapshot(
    // UUID — стабильный ключ призрака: сетевой entityId меняется при
    // выгрузке/загрузке энтити на границах прогруза
    val uuid: SerializedUUID,
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
data class ProjectileSnapshot(
    val uuid: SerializedUUID,
    val entityId: Int,
    val type: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val vx: Double,
    val vy: Double,
    val vz: Double,
    val gravity: Float,
)

@Serializable
data class DistantVehiclesMessage(
    val interval: Int,
    val vehicles: List<VehicleSnapshot>,
    val projectiles: List<ProjectileSnapshot>,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        DistantVehicleManager.handleMessage(this@DistantVehiclesMessage)
    }
}
