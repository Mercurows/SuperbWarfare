package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import java.util.*

@RegistryName("electric_baton")
class ElectricBatonItem : GeoGunItem(Properties()) {

    override fun isDamageable(stack: ItemStack): Boolean = true

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.empty()
    }
}
