package com.atsuishio.superbwarfare.capability.energy

import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.IEnergyStorage

class ItemEnergyProvider(stack: ItemStack, private val capability: LazyOptional<IEnergyStorage>) :
    ICapabilityProvider {
    constructor(stack: ItemStack, energyCapacity: Int) : this(
        stack,
        LazyOptional.of<IEnergyStorage> { ItemEnergyStorage(stack, energyCapacity) }
    )

    override fun <T> getCapability(cap: Capability<T>, dire: Direction?): LazyOptional<T> {
        return ForgeCapabilities.ENERGY.orEmpty<T>(cap, capability)
    }
}
