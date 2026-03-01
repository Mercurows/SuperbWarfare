package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.data.gun.Ammo
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

class HandgunAmmoBoxItem : AmmoSupplierItem(Ammo.HANDGUN, 30, Properties()) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced)
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.handgun_ammo_box").withStyle(ChatFormatting.GRAY)
        )
    }
}
