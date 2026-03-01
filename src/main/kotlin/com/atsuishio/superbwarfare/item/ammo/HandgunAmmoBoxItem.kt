package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.data.gun.Ammo
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

class HandgunAmmoBoxItem : AmmoSupplierItem(Ammo.HANDGUN, 30, Properties()) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag)
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.handgun_ammo_box").withStyle(ChatFormatting.GRAY)
        )
    }
}
