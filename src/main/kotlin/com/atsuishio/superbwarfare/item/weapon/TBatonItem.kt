package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import net.minecraft.world.item.ItemStack

@RegistryName("t_baton")
class TBatonItem : GeoGunItem(Properties()) {
    override fun isDamageable(stack: ItemStack?): Boolean = true
}
