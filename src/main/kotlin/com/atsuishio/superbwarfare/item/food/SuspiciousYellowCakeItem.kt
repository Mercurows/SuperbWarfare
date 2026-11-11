package com.atsuishio.superbwarfare.item.food

import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

@RegistryName("suspicious_yellow_cake")
class SuspiciousYellowCakeItem :
    Item(
        Properties().food(
            FoodProperties.Builder().nutrition(20).saturationMod(0.5f)
                .effect({ MobEffectInstance(ModMobEffects.RADIATION.get(), 3600, 2) }, 1.0f)
                .build()
        ).rarity(Rarity.RARE)
    ) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.suspicious_yellow_cake").withStyle(ChatFormatting.GRAY)
                .withStyle(ChatFormatting.ITALIC)
        )
    }
}
