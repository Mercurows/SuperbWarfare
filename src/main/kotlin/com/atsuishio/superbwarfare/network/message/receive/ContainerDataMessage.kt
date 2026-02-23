package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.inventory.menu.EnergyMenu
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Code based on @GoryMoon's Chargers
 */
@Serializable
data class ContainerDataMessage(
    val containerId: Int,
    val data: List<@Serializable(ContainerDataPairSerializer::class) Pair>
) : ClientPacketPayload() {

    class Pair(val id: Int, val data: Long)

    override fun PayloadContext.handler() {
        val localPlayer = localPlayer ?: return
        if (localPlayer.containerMenu.containerId == containerId) {
            data.forEach { p -> (localPlayer.containerMenu as EnergyMenu).setData(p.id, p.data) }
        }
    }

}


private object ContainerDataPairSerializer : KSerializer<ContainerDataMessage.Pair> {
    override val descriptor = buildClassSerialDescriptor("ContainerDataPair") {
        element<Int>("id")
        element<Long>("data")
    }

    override fun serialize(encoder: Encoder, value: ContainerDataMessage.Pair) {
        encoder.encodeInt(value.id)
        encoder.encodeLong(value.data)
    }

    override fun deserialize(decoder: Decoder): ContainerDataMessage.Pair {
        return ContainerDataMessage.Pair(decoder.decodeInt(), decoder.decodeLong())
    }
}
