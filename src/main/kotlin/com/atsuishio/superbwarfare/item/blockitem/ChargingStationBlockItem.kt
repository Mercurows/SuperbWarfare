package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.client.tooltip.component.ChargingStationImageComponent
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlocks
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.math.roundToInt

class ChargingStationBlockItem : BlockItem(ModBlocks.CHARGING_STATION.get(), Properties().stacksTo(1)) {
    override fun isBarVisible(pStack: ItemStack): Boolean {
        val tag = getBlockEntityData(pStack)
        val energy = tag?.getInt("Energy") ?: 0
        return energy != MiscConfig.CHARGING_STATION_MAX_ENERGY.get() && energy != 0
    }

    override fun getBarWidth(pStack: ItemStack): Int {
        val tag = getBlockEntityData(pStack)
        val energy = tag?.getInt("Energy") ?: 0
        return (energy * 13F / 1.coerceAtLeast(MiscConfig.CHARGING_STATION_MAX_ENERGY.get())).roundToInt()
    }

    override fun getBarColor(pStack: ItemStack): Int {
        return 0xFFFF00
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(ChargingStationImageComponent(pStack))
    }
}
