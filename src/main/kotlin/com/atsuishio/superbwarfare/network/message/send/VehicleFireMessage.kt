package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@RegisterPacket
@Serializable
data class VehicleFireMessage(
    val uuid: SerializedUUID?,
    val targetPos: SerializedVector3f?,
    val weaponName: String? = null,
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

        applyClientShotDirection(vehicle, player)
        try {
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
        } finally {
            vehicle.clientShotDirection = null
        }
    }

    private fun applyClientShotDirection(vehicle: VehicleEntity, player: net.minecraft.world.entity.player.Player) {
        val dir = shootDirection ?: return
        if (!vehicle.hasTurret() || vehicle.turretControllerIndex != vehicle.getSeatIndex(player)) return
        if (!dir.x.isFinite() || !dir.y.isFinite() || !dir.z.isFinite()) return
        val lenSqr = dir.x * dir.x + dir.y * dir.y + dir.z * dir.z
        if (lenSqr < 0.25f || lenSqr > 4.0f) return
        val v = Vec3(dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble()).normalize()
        vehicle.clientShotDirection = v
    }
}