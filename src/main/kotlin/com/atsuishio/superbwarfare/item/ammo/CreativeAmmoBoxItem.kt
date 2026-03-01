package com.atsuishio.superbwarfare.item.ammo

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag

class CreativeAmmoBoxItem : Item(Properties().rarity(Rarity.EPIC).stacksTo(1)) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component?>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_ammo_box").withStyle(ChatFormatting.GRAY)
        )
    }
}
