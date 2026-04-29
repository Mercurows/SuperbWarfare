package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.core.component.DataComponents
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

        val colors = colors.map { it.toList() }
        stack.set(ModDataComponents.DOG_TAG_IMAGE, colors)

        if (!name.isEmpty()) {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name))
        }
    }
}
