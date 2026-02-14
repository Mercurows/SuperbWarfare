package com.atsuishio.superbwarfare.capability.energy

import net.minecraftforge.energy.IEnergyStorage

/**
 * 无限供电能力，纯逆天
 */
class InfinityEnergyStorage : IEnergyStorage {
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        return 0
    }

    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        return maxExtract
    }

    override fun getEnergyStored(): Int {
        return Int.MAX_VALUE
    }

    override fun getMaxEnergyStored(): Int {
        return Int.MAX_VALUE
    }

    override fun canExtract(): Boolean {
        return true
    }

    override fun canReceive(): Boolean {
        return false
    }
}
