package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable
import net.minecraft.world.entity.LivingEntity

@Serializable
data class ClientPhosphorusFireMessage(
    val id: Int,
    val flag: Boolean,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(id) as? LivingEntity ?: return
        PhosphorusFireCapability.of(entity).isOnFire = flag
    }
}
