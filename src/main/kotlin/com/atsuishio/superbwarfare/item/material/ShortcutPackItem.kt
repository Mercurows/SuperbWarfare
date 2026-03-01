package com.atsuishio.superbwarfare.item.material

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class ShortcutPackItem : Item(Properties().rarity(Rarity.EPIC)) {
    override fun appendHoverText(stack: ItemStack, level: Level?, list: MutableList<Component>, flag: TooltipFlag) {
        list.add(Component.translatable("des.superbwarfare.shortcut_pack_2").withStyle(ChatFormatting.AQUA))
        list.add(Component.translatable("des.superbwarfare.shortcut_pack_1").withStyle(ChatFormatting.GRAY))
    }
}
