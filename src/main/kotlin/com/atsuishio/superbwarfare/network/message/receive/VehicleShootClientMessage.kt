package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.api.event.ClientVehicleFireEvent
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.localPlayer
import kotlinx.serialization.Serializable
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

@Serializable
data class VehicleShootClientMessage(
    val shooter: SerializedUUID,
    val vehicle: SerializedUUID,
    val index: Int,
    val weaponName: String = ""
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val player = localPlayer ?: return

        val s = EntityFindUtil.findPlayer(player.level(), shooter.toString())
        val v = EntityFindUtil.findEntity(player.level(), vehicle.toString())

        if (v is VehicleEntity) {
            val name = weaponName.ifEmpty { null }
            FORGE_BUS.post(s?.let { ClientVehicleFireEvent(v, it, index, name) })
        }
    }
}
