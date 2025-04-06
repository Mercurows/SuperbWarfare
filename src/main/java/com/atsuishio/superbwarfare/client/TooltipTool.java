package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
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
        var perkInstance = GunData.from(stack).perk.getInstance(Perk.Type.AMMO);
        return perkInstance != null && perkInstance.perk() == ModPerks.HE_BULLET.get();
    }

    public static int heBulletLevel(ItemStack stack) {
        var perkInstance = GunData.from(stack).perk.getInstance(Perk.Type.AMMO);
        if (perkInstance != null && perkInstance.perk() == ModPerks.HE_BULLET.get()) {
            return perkInstance.level();
        }
        return 0;
    }
}
