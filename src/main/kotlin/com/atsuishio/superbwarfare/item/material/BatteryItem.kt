package com.atsuishio.superbwarfare.item.material

import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import java.util.*
import kotlin.math.roundToInt

open class BatteryItem(var maxEnergy: Int, properties: Properties) : Item(properties.stacksTo(1)) {
    override fun isBarVisible(pStack: ItemStack): Boolean {
        val cap = pStack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return false
        return cap.energyStored != cap.maxEnergyStored
    }

    override fun getBarWidth(pStack: ItemStack): Int {
        var energy = 0
        val cap = pStack.getCapability(Capabilities.EnergyStorage.ITEM)
        if (cap != null) {
            energy = cap.energyStored
        }

        return (energy * 13f / maxEnergy).roundToInt()
    }

    override fun getBarColor(pStack: ItemStack) = 0xFFFF00

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of(CellImageComponent(pStack))
    }

    fun makeFullEnergyStack(): ItemStack {
        val stack = ItemStack(this)
        val cap = stack.getCapability(Capabilities.EnergyStorage.ITEM) ?: return stack

        cap.receiveEnergy(maxEnergy, false)
        return stack
    }
}