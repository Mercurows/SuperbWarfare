package com.atsuishio.superbwarfare.capability.energy

import com.atsuishio.superbwarfare.init.ModDataComponents
import net.minecraft.world.item.ItemStack
import java.util.function.Function

class ItemEnergyStorage(
    private val stack: ItemStack,
    capacityGetter: Function<ItemStack, Int>,
    maxReceiveGetter: Function<ItemStack, Int>,
    maxExtractGetter: Function<ItemStack, Int>
) : DynamicEnergyStorage(
    { capacityGetter.apply(stack) }, { maxReceiveGetter.apply(stack) }, { maxExtractGetter.apply(stack) }) {

    @JvmOverloads
    constructor(stack: ItemStack, capacity: Int, maxReceive: Int = capacity, maxExtract: Int = capacity) : this(
        stack,
        { _ -> capacity },
        { _ -> maxReceive },
        { _ -> maxExtract })

    init {
        val component = stack.get(ModDataComponents.ENERGY)
        this.energy = component ?: 0
    }

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val received = super.receiveEnergy(maxReceive, simulate)

        if (received > 0 && !simulate) {
            stack.set<Int?>(ModDataComponents.ENERGY, energyStored)
        }

        return received
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val extracted = super.extractEnergy(maxExtract, simulate)

        if (extracted > 0 && !simulate) {
            stack.set(ModDataComponents.ENERGY, energyStored)
        }

        return extracted
    }
}
