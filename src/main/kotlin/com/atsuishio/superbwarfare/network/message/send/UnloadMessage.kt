package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool

object UnloadMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        val data = from(stack)
        data.withdrawAmmo(player)
        data.save()
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f)
    }
}
