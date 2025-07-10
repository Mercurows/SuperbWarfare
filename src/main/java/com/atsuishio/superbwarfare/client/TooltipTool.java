package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TooltipTool {

    public static void addHideText(List<Component> tooltip, Component text) {
        if (Screen.hasShiftDown()) {
            tooltip.add(text);
        }
    }

    public static void addDevelopingText(List<Component> tooltip) {
        tooltip.add(Component.translatable("des.superbwarfare.developing").withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.BOLD));
    }

    public static void addScreenProviderText(List<Component> tooltip) {
        tooltip.add(Component.translatable("des.superbwarfare.item_screen_provider",
                "[" + ModKeyMappings.EDIT_MODE.getKey().getDisplayName().getString() + "]").withStyle(ChatFormatting.AQUA));
    }
}
