package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.ActiveGun
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class FireModeMessage(val forward: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = ActiveGun.stackOf(player)

        if (stack.item !is GunItem) return
        val data = from(stack)
        if (data.reloading()) return

        val selectedFireMode = data.selectedFireMode.get()
        val fireModes = data.get(GunProp.AVAILABLE_FIRE_MODES)

        if (fireModes.size > 1) {
            val mode = (selectedFireMode + (if (forward) -1 else 1) + fireModes.size) % fireModes.size
            data.selectedFireMode.set(mode)
            SoundTool.playLocalSound(player, ModSounds.FIRE_RATE.get())
            return
        }
        data.save()
    }
}
