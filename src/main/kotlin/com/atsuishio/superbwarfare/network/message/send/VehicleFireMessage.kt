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
    val weaponName: String? = null,
    /** 非骑乘遥控发射时，指定发射载具的实体 ID */
    val shooterVehicleId: Int? = null,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = if (shooterVehicleId != null) {
            player.level().getEntity(shooterVehicleId) as? VehicleEntity
        } else {
            player.vehicle as? VehicleEntity
        } ?: return

        if (targetPos != null) {
            // Map strike: fire a specific weapon by name
            if (weaponName != null) {
                vehicle.vehicleShoot(player, weaponName, uuid, Vec3(targetPos))
            } else {
                vehicle.vehicleShoot(player, uuid, Vec3(targetPos))
            }
        } else {
            vehicle.vehicleShoot(player, uuid, null)
        }
    }
}
