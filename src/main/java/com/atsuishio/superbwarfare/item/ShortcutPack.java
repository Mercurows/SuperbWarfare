package com.atsuishio.superbwarfare.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class ShortcutPack extends Item {
    public ShortcutPack() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.use_tip.shortcut_pack").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("des.superbwarfare.tips.shortcut_pack").withStyle(ChatFormatting.GRAY));
    }

}
