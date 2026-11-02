package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.special.HammerItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity

@RegistryName("netherite_hammer")
class NetheriteHammerItem : HammerItem(Properties().fireResistant().rarity(Rarity.EPIC)) {
    override fun isDamageable(stack: ItemStack?) = false
}
