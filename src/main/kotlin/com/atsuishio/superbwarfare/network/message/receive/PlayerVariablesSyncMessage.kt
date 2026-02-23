package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.clientLevel
import kotlinx.serialization.Serializable

@Serializable
data class PlayerVariablesSyncMessage(
    val target: Int,
    val data: Map<Byte, Int>,
) : ClientPacketPayload() {

    override fun PayloadContext.handler() {
        val entity = clientLevel?.getEntity(target) ?: return

        val variables = entity.getCapability(ModCapabilities.PLAYER_VARIABLE, null).orElse(PlayerVariable())
        for (entry in data.entries) {
            val type = entry.key

            if (type.toInt() == -1) {
                variables.activeThermalImaging = entry.value == 1
            } else {
                val types = Ammo.entries.toTypedArray()
                if (type < types.size) {
                    types[type.toInt()].set(variables, entry.value)
                }
            }
        }
    }
}
