package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModAttachments
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

        val variables = entity.getData(ModAttachments.PLAYER_VARIABLE)

        for ((type, value) in data) {
            if (type == (-1).toByte()) {
                variables.activeThermalImaging = value == 1
            } else {
                val types = Ammo.entries.toTypedArray()
                if (type < types.size) {
                    types[type.toInt()].set(variables, value)
                }
            }
        }

        entity.setData(ModAttachments.PLAYER_VARIABLE, variables)
    }
}
