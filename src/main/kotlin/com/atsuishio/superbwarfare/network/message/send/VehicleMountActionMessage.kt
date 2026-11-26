package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class VehicleMountActionMessage(
    val vehicleId: Int,
    val pressed: Boolean,
    val gunId: SerializedUUID? = null,
    val renew: Boolean = false,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (pressed && player.hasEffect(ModMobEffects.SHOCK.get())) return
        val vehicle = player.vehicle as? RCEntity ?: return
        if (vehicle.id != vehicleId) return
        val seat = vehicle.getSeatIndex(player)
        if (seat < 0 || vehicle.hasWeapon(seat)) return
        if (gunId != null) vehicle.mounts.gunTrigger(player, gunId, pressed, renew)
        else if (pressed && !renew) vehicle.mounts.use(player)
    }
}
