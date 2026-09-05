package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable

@Serializable
@RegisterPacket
data class ClientInfiniteAmmoMessage(val id: Int, val flag: Boolean) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(id) ?: return

        InfiniteAmmoCapability.modify(entity) {
            it.hasInfinityAmmo = flag
        }
    }
}
