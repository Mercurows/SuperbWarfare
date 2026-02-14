package com.atsuishio.superbwarfare.capability.energy

import net.minecraftforge.energy.EnergyStorage

open class DynamicEnergyStorage @JvmOverloads constructor(
    protected val maxStorageGetter: () -> Int,
    protected val maxReceiveGetter: () -> Int = maxStorageGetter,
    protected val maxExtractGetter: () -> Int = maxStorageGetter
) : EnergyStorage(
    Int.MAX_VALUE
) {
    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        updateProps()
        return super.extractEnergy(maxExtract, simulate)
    }

    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        updateProps()
        return super.receiveEnergy(maxReceive, simulate)
    }

    override fun canReceive(): Boolean {
        updateProps()
        return super.canReceive()
    }

    override fun canExtract(): Boolean {
        updateProps()
        return super.canExtract()
    }

    override fun getMaxEnergyStored(): Int {
        updateProps()
        return super.getMaxEnergyStored()
    }

    protected fun updateProps() {
        this.capacity = maxStorageGetter()
        this.maxExtract = maxExtractGetter()
        this.maxReceive = maxReceiveGetter()
    }
}
