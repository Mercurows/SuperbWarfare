package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
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

    public static double perkDamage(ItemStack stack) {
        var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.damageRate;
        }
        return 1;
    }

    public static boolean heBullet(ItemStack stack) {
        var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);
        return perk == ModPerks.HE_BULLET.get();
    }

    public static int heBulletLevel(ItemStack stack) {
        var perk = PerkHelper.getPerkByType(stack, Perk.Type.AMMO);
        if (perk == ModPerks.HE_BULLET.get()) {
            return PerkHelper.getItemPerkLevel(perk, stack);
        }
        return 0;
    }
}
