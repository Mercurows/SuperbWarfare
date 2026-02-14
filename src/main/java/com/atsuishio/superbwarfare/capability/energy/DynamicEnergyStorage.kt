package com.atsuishio.superbwarfare.capability.energy

import net.neoforged.neoforge.energy.EnergyStorage
import java.util.function.Supplier

open class DynamicEnergyStorage @JvmOverloads constructor(
    protected val maxStorageGetter: Supplier<Int?>,
    protected val maxReceiveGetter: Supplier<Int?> = maxStorageGetter,
    protected val maxExtractGetter: Supplier<Int?> = maxStorageGetter
) : EnergyStorage(Int.MAX_VALUE) {
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
        this.capacity = maxStorageGetter.get()!!
        this.maxExtract = maxExtractGetter.get()!!
        this.maxReceive = maxReceiveGetter.get()!!
    }
}
