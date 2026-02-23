package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable

@Serializable
data class ZoomMessage(val msgType: Int) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return

        // 缩放音效播放条件: 载具是武器载具，且该位置有可用武器
        if (vehicle.hasWeapon(vehicle.getSeatIndex(player)) && vehicle.banHand(player)) {
            if (msgType == 0) {
                SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2f, 1f)
            } else if (msgType == 1) {
                SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_OUT.get(), 2f, 1f)
            }
        }
    }
}
