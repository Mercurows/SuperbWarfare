package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.curio.ParachuteItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import net.minecraft.sounds.SoundSource
import top.theillusivec4.curios.api.CuriosApi

object ParachuteMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        CuriosApi.getCuriosInventory(player).ifPresent { c ->
            c.findFirstCurio(ModItems.PARACHUTE.get()).ifPresent { s ->
                val stack = s.stack()
                if (!player.cooldowns.isOnCooldown(stack.item)) {
                    val tag = stack.getOrCreateTag()
                    if (!tag.getBoolean(ParachuteItem.TAG_OPEN) && player.deltaMovement.y < -0.6 && player.fallDistance > 4) {
                        tag.putBoolean(ParachuteItem.TAG_OPEN, true)
                        player.cooldowns.addCooldown(stack.item, 10)
                        player.level().playSound(
                            null,
                            player.x,
                            player.y,
                            player.z,
                            ModSounds.PARACHUTE_OPEN.get(),
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                    } else if (tag.getBoolean(ParachuteItem.TAG_OPEN)) {
                        tag.putBoolean(ParachuteItem.TAG_OPEN, false)
                        player.cooldowns.addCooldown(stack.item, 10)
                        player.level().playSound(
                            null,
                            player.x,
                            player.y,
                            player.z,
                            ModSounds.PARACHUTE_CLOSE.get(),
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                    }
                }
            }
        }
    }
}