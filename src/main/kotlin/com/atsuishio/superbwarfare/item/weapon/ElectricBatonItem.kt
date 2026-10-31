package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import net.minecraft.world.item.ItemStack

@RegistryName("electric_baton")
class ElectricBatonItem : GeoGunItem(Properties()) {

    override fun isDamageable(stack: ItemStack?): Boolean = true
}
