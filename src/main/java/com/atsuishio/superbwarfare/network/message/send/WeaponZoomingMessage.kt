package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class WeaponZoomingMessage(val zooming: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val stack = sender().mainHandItem
        if (stack.item !is GunItem) return

        from(stack).zooming.set(zooming)
    }
}
