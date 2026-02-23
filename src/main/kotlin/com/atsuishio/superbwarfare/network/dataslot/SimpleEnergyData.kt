package com.atsuishio.superbwarfare.network.dataslot

/**
 * Code based on @GoryMoon's Chargers
 */
class SimpleEnergyData(size: Int) : ContainerEnergyData {
    val data: LongArray = LongArray(size)

    override fun get(index: Int): Long {
        return this.data[index]
    }

    override fun set(index: Int, value: Long) {
        this.data[index] = value
    }

    override fun getCount(): Int {
        return this.data.size
    }
}
