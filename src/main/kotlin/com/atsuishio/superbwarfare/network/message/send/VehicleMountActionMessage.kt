package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
object VehicleMountActionMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.hasEffect(ModMobEffects.SHOCK.get())) return
        val vehicle = player.vehicle as? RCEntity ?: return
        val seat = vehicle.getSeatIndex(player)
        if (!vehicle.hasWeapon(seat)) vehicle.mounts.use(player)
    }
}
