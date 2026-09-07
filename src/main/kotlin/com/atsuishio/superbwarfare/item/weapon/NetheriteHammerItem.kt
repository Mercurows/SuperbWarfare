package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Tiers

@RegistryName("netherite_hammer")
class NetheriteHammerItem : HammerItem(Tiers.NETHERITE, 75, -3.5f, Properties().durability(2800).fireResistant()) {
    override fun isDamageable(stack: ItemStack?) = false
}
