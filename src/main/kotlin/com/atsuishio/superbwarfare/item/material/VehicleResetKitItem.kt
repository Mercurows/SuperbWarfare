package com.atsuishio.superbwarfare.item.material

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import javax.annotation.ParametersAreNonnullByDefault

class VehicleResetKitItem : Item(Properties().rarity(Rarity.UNCOMMON).stacksTo(1)) {
    override fun hasCraftingRemainingItem(stack: ItemStack): Boolean {
        return true
    }

    override fun getCraftingRemainingItem(itemstack: ItemStack): ItemStack {
        return itemstack.copy()
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.vehicle_reset_kit_1").withStyle(ChatFormatting.AQUA)
        )
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.vehicle_reset_kit_2").withStyle(ChatFormatting.GRAY)
        )
    }
}
