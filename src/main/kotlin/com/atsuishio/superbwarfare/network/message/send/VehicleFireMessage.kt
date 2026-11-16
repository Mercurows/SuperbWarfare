package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
@RegisterPacket
data class VehicleFireMessage(
    val uuid: SerializedUUID?,
    val targetPos: SerializedVector3f?,
    val weaponName: String? = null,
    /** 非骑乘遥控发射时，指定发射载具的实体 ID */
    val shooterVehicleId: Int? = null,
    val shootDirection: SerializedVector3f? = null,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = if (shooterVehicleId != null) {
            player.level().getEntity(shooterVehicleId) as? VehicleEntity
        } else {
            player.vehicle as? VehicleEntity
        } ?: return

        val target = targetPos?.let { Vec3(it) }
        if (target != null && weaponName != null) {
            vehicle.vehicleShoot(player, weaponName, uuid, target)
        } else if (shooterVehicleId == null && shootDirection != null) {
            vehicle.withClientShotDirection(player, Vec3(shootDirection)) {
                vehicle.vehicleShoot(player, uuid, target)
            }
        } else {
            vehicle.vehicleShoot(player, uuid, target)
        }
    }
}
