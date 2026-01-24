package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.init.ModKeyMappings
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object TooltipTool {
    @JvmStatic
    fun addHideText(tooltip: MutableList<Component>, text: Component) {
        if (Screen.hasShiftDown()) {
            tooltip += text
        }
    }

    @JvmStatic
    fun addDevelopingText(tooltip: MutableList<Component>) {
        tooltip += Component.translatable("des.superbwarfare.developing")
            .withStyle(ChatFormatting.LIGHT_PURPLE)
            .withStyle(ChatFormatting.BOLD)
    }

    @JvmStatic
    fun addScreenProviderText(tooltip: MutableList<Component>) {
        tooltip += Component.translatable(
            "des.superbwarfare.item_screen_provider",
            "[${ModKeyMappings.EDIT_MODE.key.displayName.string}]"
        ).withStyle(ChatFormatting.AQUA)
    }
}