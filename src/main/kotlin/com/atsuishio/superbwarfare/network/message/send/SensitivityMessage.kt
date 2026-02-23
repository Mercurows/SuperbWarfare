package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import kotlin.math.max
import kotlin.math.min

@Serializable
data class SensitivityMessage(val isAdd: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val data = from(stack)
        if (isAdd) {
            data.sensitivity.set(min(10, data.sensitivity.get() + 1))
        } else {
            data.sensitivity.set(max(-10, data.sensitivity.get() - 1))
        }
        data.save()
        player.displayClientMessage(
            Component.translatable("tips.superbwarfare.sensitivity", data.sensitivity.get()),
            true
        )
    }
}
