package com.atsuishio.superbwarfare.item

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.ItemStack
import java.util.*

@Deprecated("reserved for compatibility, DO NOT USE")
sealed interface LungeMine

@RegistryName("lunge_mine")
open class LungeMineItem : GeoGunItem(Properties()), @Suppress("DEPRECATION") LungeMine {

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.empty()
    }
}
