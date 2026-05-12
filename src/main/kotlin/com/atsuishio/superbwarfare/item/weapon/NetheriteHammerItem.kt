package com.atsuishio.superbwarfare.item.weapon

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Tiers
import net.minecraft.world.item.component.Unbreakable

class NetheriteHammerItem : HammerItem(
    Tiers.NETHERITE, 75, -3.5f, Properties().durability(2800).fireResistant()
        .component(DataComponents.UNBREAKABLE, Unbreakable(false))
) {
    override fun isDamageable(stack: ItemStack) = false
}