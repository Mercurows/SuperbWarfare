package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Tiers
import net.minecraft.world.item.component.Unbreakable

@RegistryName("netherite_hammer")
class NetheriteHammerItem : HammerItem(
    Tiers.NETHERITE, 75, -3.5f, Properties().durability(2800).fireResistant()
        .component(DataComponents.UNBREAKABLE, Unbreakable(false))
) {
    override fun isDamageable(stack: ItemStack) = false
}
