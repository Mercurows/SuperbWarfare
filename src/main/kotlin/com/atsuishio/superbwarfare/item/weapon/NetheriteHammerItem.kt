package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.special.HammerItem
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SwordItem.createAttributes
import net.minecraft.world.item.Tiers
import net.minecraft.world.item.component.Unbreakable

@RegistryName("netherite_hammer")
class NetheriteHammerItem : HammerItem(
    Properties().durability(2800).fireResistant()
        .component(DataComponents.UNBREAKABLE, Unbreakable(false))
        .attributes(createAttributes(Tiers.NETHERITE, 75, -3.5f))
) {
    override fun isDamageable(stack: ItemStack?): Boolean = false
}
