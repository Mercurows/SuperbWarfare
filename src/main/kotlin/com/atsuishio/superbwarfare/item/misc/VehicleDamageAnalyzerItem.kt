package com.atsuishio.superbwarfare.item.misc

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import javax.annotation.ParametersAreNonnullByDefault

class VehicleDamageAnalyzerItem : Item(Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.vehicle_damage_analyzer").withStyle(ChatFormatting.GRAY)
        )
    }
}