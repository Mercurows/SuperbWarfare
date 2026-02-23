package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.SoundTool
import kotlinx.serialization.Serializable
import net.minecraftforge.common.capabilities.ForgeCapabilities

@Serializable
data class FireModeMessage(val forward: Boolean) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem

        if (stack.item !is GunItem) return
        val data = from(stack)

        val selectedFireMode = data.selectedFireMode.get()
        val fireModes = data.get(GunProp.AVAILABLE_FIRE_MODES)

        if (fireModes.size > 1) {
            val mode = (selectedFireMode + (if (forward) -1 else 1) + fireModes.size) % fireModes.size
            data.selectedFireMode.set(mode)
            SoundTool.playLocalSound(player, ModSounds.FIRE_RATE.get())
            return
        }

        if (stack.item === ModItems.SENTINEL.get()
            && !player.isSpectator
            && !(player.cooldowns.isOnCooldown(stack.item))
            && data.reload.time() == 0
            && !data.charging()
        ) {
            for (cell in player.inventory.items) {
                if (cell.`is`(ModItems.CELL.get())) {
                    val flag = booleanArrayOf(false)
                    cell.getCapability(ForgeCapabilities.ENERGY).ifPresent { iEnergyStorage ->
                        flag[0] = iEnergyStorage.energyStored >= 0
                    }

                    if (flag[0]) {
                        data.charge.starter.markStart()
                    }
                }
            }
        }

        if (stack.item === ModItems.JAVELIN.get()) {
            SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_OUT.get())
        }
        data.save()
    }
}
