package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level

@RegistryName("rifle_ammo_box")
class RifleAmmoBoxItem : AmmoSupplierItem(Ammo.RIFLE, 30, Properties()) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced)
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.rifle_ammo_box").withStyle(ChatFormatting.GRAY)
        )
    }
}
