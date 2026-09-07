package com.atsuishio.superbwarfare.item.food

import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

@RegistryName("crust")
class CrustItem : Item(Properties().food(FoodProperties.Builder().nutrition(10).saturationMod(0.5f).build())) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(Component.translatable("des.superbwarfare.crust").withStyle(ChatFormatting.GRAY))
    }
}
