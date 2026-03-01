package com.atsuishio.superbwarfare.item.material

import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider
import com.atsuishio.superbwarfare.client.tooltip.component.CellImageComponent
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import java.util.*
import kotlin.math.roundToInt

open class BatteryItem(var maxEnergy: Int, properties: Properties) : Item(properties.stacksTo(1)) {
    private val energyCapacity: () -> Int = { maxEnergy }

    override fun isBarVisible(pStack: ItemStack): Boolean {
        return pStack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.energyStored }
            .orElse(0) != maxEnergy
    }

    override fun getBarWidth(pStack: ItemStack): Int {
        val energy = pStack.getCapability(ForgeCapabilities.ENERGY)
            .map { it.energyStored }
            .orElse(0)

        return (energy * 13f / maxEnergy).roundToInt()
    }

    override fun initCapabilities(stack: ItemStack, tag: CompoundTag?): ICapabilityProvider {
        return ItemEnergyProvider(stack, energyCapacity())
    }

    override fun getBarColor(pStack: ItemStack): Int {
        return 0xFFFF00
    }

    override fun getTooltipImage(pStack: ItemStack): Optional<TooltipComponent> {
        return Optional.of<TooltipComponent>(CellImageComponent(pStack))
    }

    fun makeFullEnergyStack(): ItemStack {
        val stack = ItemStack(this)
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent { it.receiveEnergy(maxEnergy, false) }
        return stack
    }
}