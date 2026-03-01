package com.atsuishio.superbwarfare.item.ammo

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class CreativeAmmoBoxItem : Item(Properties().rarity(Rarity.EPIC).stacksTo(1)) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_ammo_box").withStyle(ChatFormatting.GRAY)
        )
    }
}
