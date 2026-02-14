package com.atsuishio.superbwarfare.item.weapon

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Tiers

class NetheriteHammerItem : HammerItem(Tiers.NETHERITE, 13, -3.2f, Properties().durability(2800).fireResistant()) {
    override fun isDamageable(stack: ItemStack) = false
}