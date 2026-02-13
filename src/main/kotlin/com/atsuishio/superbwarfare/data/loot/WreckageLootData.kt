package com.atsuishio.superbwarfare.data.loot

class WreckageLootData(val pools: List<Pool>) {
    class Pool(val entries: List<Entry> = listOf(), val rolls: Int = 1, val source: String)

    class Entry(val name: String, val count: Int = 1, val chance: Double = 1.0)
}