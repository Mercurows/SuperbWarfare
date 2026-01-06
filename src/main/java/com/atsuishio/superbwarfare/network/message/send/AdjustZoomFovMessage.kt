package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.toGunData
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import kotlin.math.roundToInt

@Serializable
data class AdjustZoomFovMessage(val scroll: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val stack = player.mainHandItem
        val gun = stack.toGunData() ?: return
        val data = gun.data()

        if (stack.`is`(ModItems.MINIGUN.get())) {
            val minRpm = (300 - 1200).toDouble()
            val maxRpm = (2400 - 1200).toDouble()

            val customRPM = data.getInt("CustomRPM")
            var targetCustomRPM = Mth.clamp(customRPM + 50 * scroll, minRpm, maxRpm).toInt()

            targetCustomRPM = if (targetCustomRPM == 1150 - 1200) {
                1145 - 1200
            } else {
                (targetCustomRPM / 50.0).roundToInt() * 50
            }

            data.putInt("CustomRPM", targetCustomRPM)

            player.displayClientMessage(Component.literal("RPM: " + format0D((customRPM + 1200).toDouble())), true)
            if (customRPM > minRpm && customRPM < maxRpm) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f)
            }
        } else {
            val minZoom = gun.minZoom() - 1.25
            val maxZoom = gun.maxZoom() - 1.25
            val customZoom = data.getDouble("CustomZoom")
            data.putDouble("CustomZoom", Mth.clamp(customZoom + 0.5 * scroll, minZoom, maxZoom))

            if (customZoom > minZoom && customZoom < maxZoom) {
                SoundTool.playLocalSound(player, ModSounds.ADJUST_FOV.get(), 1f, 0.7f)
            }
        }

        gun.save()
    }
}
