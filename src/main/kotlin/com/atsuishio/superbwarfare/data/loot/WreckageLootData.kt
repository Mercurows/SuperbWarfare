package com.atsuishio.superbwarfare.data.loot

import com.atsuishio.superbwarfare.network.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraftforge.registries.ForgeRegistries

@Serializable
class WreckageLootData(
    @SerialName("ID") val id: SerializedResourceLocation,
    @SerialName("Pools") val pools: List<Pool>
) {
    @Serializable
    class Pool(
        @SerialName("Entries") val entries: List<Entry> = listOf(),
        @SerialName("Rolls") val rolls: Int = 1,
        @SerialName("Source") val source: String = "@Default"
    ) {
        constructor(builder: Builder) : this(builder.entries, builder.rolls, builder.source)

        class Builder(val rolls: Int = 1, val source: String = "@Default") {
            val entries = mutableListOf<Entry>()

            fun addEntry(entry: Entry): Builder {
                entries.add(entry)
                return this
            }

            fun build(): Pool {
                return Pool(entries, rolls, source)
            }
        }

    }

    @Serializable
    class Entry(
        @SerialName("Name") val name: String,
        @SerialName("Count") val count: Int = 1,
        @SerialName("Chance") val chance: Double = 1.0
    ) {
        constructor(item: Item, count: Int = 1, chance: Double = 1.0) : this(
            ForgeRegistries.ITEMS.getKey(item)!!.toString(), count, chance
        )

    }

    class Builder {
        val pools = mutableListOf<Pool>()

        fun addPool(pool: Pool): Builder {
            pools.add(pool)
            return this
        }

        fun build(id: ResourceLocation): WreckageLootData {
            return WreckageLootData(id, pools)
        }
    }

}