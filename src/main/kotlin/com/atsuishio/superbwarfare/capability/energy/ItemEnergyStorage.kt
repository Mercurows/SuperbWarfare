package com.atsuishio.superbwarfare.capability.energy

import net.minecraft.world.item.ItemStack

class ItemEnergyStorage(
    private val stack: ItemStack,
    capacityGetter: (ItemStack) -> (Int),
    maxReceiveGetter: (ItemStack) -> (Int),
    maxExtractGetter: (ItemStack) -> (Int)
) : DynamicEnergyStorage({
    capacityGetter(stack)
}, { maxReceiveGetter(stack) }, { maxExtractGetter(stack) }) {
    @JvmOverloads
    constructor(stack: ItemStack, capacity: Int, maxReceive: Int = capacity, maxExtract: Int = capacity) : this(
        stack,
        { _ -> capacity },
        { _ -> maxReceive },
        { _ -> maxExtract }
    )

    init {
        if (stack.tag != null) {
            this.energy = if (stack.hasTag() && stack.tag!!.contains(NBT_ENERGY)) stack.tag!!.getInt(NBT_ENERGY)
            else 0
        }
    }

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val received = super.receiveEnergy(maxReceive, simulate)

        if (received > 0 && !simulate) {
            stack.getOrCreateTag().putInt(NBT_ENERGY, energyStored)
        }

        return received
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val extracted = super.extractEnergy(maxExtract, simulate)

        if (extracted > 0 && !simulate) {
            stack.getOrCreateTag().putInt(NBT_ENERGY, energyStored)
        }

        return extracted
    }

    companion object {
        private const val NBT_ENERGY = "Energy"
    }
}
