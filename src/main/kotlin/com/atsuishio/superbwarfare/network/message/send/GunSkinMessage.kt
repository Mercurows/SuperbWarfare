package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun_skin.GunSkin
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/** Changes only the skin of the same main-hand stack that the editor displayed. */
@RegisterPacket
@Serializable
data class GunSkinMessage(val gunId: String, val gunUuid: String?, val hotbarSlot: Int, val skinId: String) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.inventory.selected != hotbarSlot) return
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        val data = GunData.from(stack)
        if (data.id != gunId || data.uuid?.toString() != gunUuid) return
        if (skinId.isNotEmpty() && GunSkin.from(data).getSkin(skinId)?.texture == null) return
        data.skinId.set(skinId)
    }
}
