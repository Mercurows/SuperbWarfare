package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

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

    public static boolean heBullet(ItemStack stack) {
        var perk = PerkHelper.getPerkByType(NBTTool.getTag(stack), Perk.Type.AMMO);
        return perk == ModPerks.HE_BULLET.get();
    }

    public static int heBulletLevel(ItemStack stack) {
        var perk = PerkHelper.getPerkByType(NBTTool.getTag(stack), Perk.Type.AMMO);
        if (perk == ModPerks.HE_BULLET.get()) {
            return PerkHelper.getItemPerkLevel(perk, NBTTool.getTag(stack));
        }
        return 0;
    }
}
