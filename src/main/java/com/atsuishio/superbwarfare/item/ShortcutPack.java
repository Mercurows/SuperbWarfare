package com.atsuishio.superbwarfare.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShortcutPack extends Item {
    public ShortcutPack() {
        super(new Properties().rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("des.superbwarfare.shortcut_pack_2").withStyle(ChatFormatting.AQUA));
        list.add(Component.translatable("des.superbwarfare.shortcut_pack_1").withStyle(ChatFormatting.GRAY));
    }

}
