package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component

@Serializable
data class DogTagFinishEditMessage(
    val colors: Array<ShortArray>,
    val name: String,
    val mainHand: Boolean,
) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val serverPlayer = sender()

        val stack = if (mainHand) serverPlayer.mainHandItem else serverPlayer.offhandItem
        if (!stack.`is`(ModItems.DOG_TAG.get())) return

        val colorsTag = CompoundTag()
        for (i in colors.indices) {
            val color = IntArray(colors[i].size)
            for (j in colors[i].indices) {
                color[j] = colors[i][j].toInt()
            }
            colorsTag.putIntArray("Color$i", color)
        }
        stack.getOrCreateTag().put("Colors", colorsTag)

        if (!name.isEmpty()) {
            stack.setHoverName(Component.literal(name))
        }
    }
}
