package com.atsuishio.superbwarfare.item.misc

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class VehicleDamageAnalyzerItem : Item(Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.vehicle_damage_analyzer").withStyle(ChatFormatting.GRAY)
        )
    }
}