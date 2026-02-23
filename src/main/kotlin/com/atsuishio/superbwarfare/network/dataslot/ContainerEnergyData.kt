package com.atsuishio.superbwarfare.network.dataslot

interface ContainerEnergyData {
    operator fun get(index: Int): Long

    operator fun set(index: Int, value: Long)

    fun getCount(): Int
}
