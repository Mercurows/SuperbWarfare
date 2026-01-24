package com.atsuishio.superbwarfare.network.dataslot

/**
 * Code based on @GoryMoon's Chargers
 */
abstract class ContainerEnergyDataSlot {
    private var prevValue: Long = 0

    companion object {
        @JvmStatic
        fun forContainer(data: ContainerEnergyData, index: Int): ContainerEnergyDataSlot {
            return object : ContainerEnergyDataSlot() {
                override fun get(): Long {
                    return data[index]
                }

                override fun set(value: Long) {
                    data[index] = value
                }
            }
        }
    }

    abstract fun get(): Long

    abstract fun set(value: Long)

    fun checkAndClearUpdateFlag(): Boolean {
        val tmp = this.get()
        val changed = tmp != this.prevValue
        this.prevValue = tmp
        return changed
    }
}
