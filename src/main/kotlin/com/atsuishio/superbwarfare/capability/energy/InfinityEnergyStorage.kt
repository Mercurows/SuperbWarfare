package com.atsuishio.superbwarfare.capability.energy

import net.neoforged.neoforge.energy.IEnergyStorage

/**
 * 无限供电能力，纯逆天
 */
class InfinityEnergyStorage : IEnergyStorage {
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean) = 0
    override fun extractEnergy(maxExtract: Int, simulate: Boolean) = maxExtract

    override fun getEnergyStored() = Int.MAX_VALUE
    override fun getMaxEnergyStored() = Int.MAX_VALUE

    override fun canExtract() = true
    override fun canReceive() = false
}
