package com.atsuishio.superbwarfare.data.loot

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageType
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
        @SerialName("Source") val source: String = "@Default",
        @SerialName("Type") val type: Type = Type.DEFAULT
    ) {
        class Builder(val rolls: Int = 1, var source: String = "@Default", var type: Type = Type.DEFAULT) {
            val entries = mutableListOf<Entry>()

            fun addEntry(entry: Entry): Builder {
                entries.add(entry)
                return this
            }

            fun addEntry(vararg entry: Entry): Builder {
                entries.addAll(entry)
                return this
            }

            fun source(source: ResourceKey<DamageType>): Builder {
                this.source = source.location().toString()
                return this
            }

            fun type(type: Type): Builder {
                this.type = type
                return this
            }

            fun build(): Pool {
                return Pool(entries, rolls, source, type)
            }
        }

        @Serializable
        enum class Type {
            @SerialName("turret_only")
            TURRET_ONLY,

            @SerialName("vehicle_only")
            VEHICLE_ONLY,

            @SerialName("complete")
            COMPLETE,

            @SerialName("default")
            DEFAULT,
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

        fun addPool(pool: Pool.Builder): Builder {
            pools.add(pool.build())
            return this
        }

        fun addPool(vararg pool: Pool): Builder {
            pools.addAll(pool)
            return this
        }

        fun addPool(vararg pool: Pool.Builder): Builder {
            pool.forEach { pools.add(it.build()) }
            return this
        }

        fun build(id: ResourceLocation): WreckageLootData {
            return WreckageLootData(id, pools)
        }
    }

}