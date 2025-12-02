package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModItems.*
import net.minecraft.core.registries.Registries
import net.minecraft.data.loot.LootTableSubProvider
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.entries.NestedLootTable
import net.minecraft.world.level.storage.loot.functions.LootItemFunction
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.function.BiConsumer

private fun containers(name: String): ResourceKey<LootTable> {
    return ResourceKey.create(Registries.LOOT_TABLE, loc("containers/$name"))
}

private fun chests(name: String): ResourceKey<LootTable> {
    return ResourceKey.create(Registries.LOOT_TABLE, loc("chests/$name"))
}

private fun special(name: String): ResourceKey<LootTable> {
    return ResourceKey.create(Registries.LOOT_TABLE, loc("special/$name"))
}


private fun singleItem(item: ItemLike, weight: Int): LootPool.Builder {
    return singleItem(item, 1f, 0f, weight, 0)
}

private fun singleItem(item: ItemLike, rolls: Float, bonus: Float, weight: Int, quality: Int): LootPool.Builder {
    return LootPool.lootPool().setRolls(ConstantValue.exactly(rolls)).setBonusRolls(ConstantValue.exactly(bonus))
        .add(LootItem.lootTableItem(item).setWeight(weight).setQuality(quality))
}

private fun multiItems(rolls: Float, bonus: Float, vararg triplet: ItemEntry): LootPool.Builder {
    val builder =
        LootPool.lootPool().setRolls(ConstantValue.exactly(rolls)).setBonusRolls(ConstantValue.exactly(bonus))
    for (t in triplet) {
        val entry: LootPoolSingletonContainer.Builder<out LootPoolSingletonContainer.Builder<*>?> =
            LootItem.lootTableItem(t.item).setWeight(t.weight).setQuality(t.quality)
        for (c in t.conditions) {
            entry.`when`(c)
        }
        for (f in t.functions) {
            entry.apply(f)
        }
        builder.add(entry)
    }
    return builder
}

private typealias ItemRegistryType = DeferredHolder<Item, *>


private operator fun BiConsumer<ResourceKey<LootTable>, LootTable.Builder>.plusAssign(builder: LootTableBuilder) {
    accept(builder.key, builder.builder)
}

private class LootTableBuilder(val key: ResourceKey<LootTable>, val builder: LootTable.Builder) {

//    fun addSingleItem(
//        item: ItemRegistryType,
//        weight: Int,
//    ) {
//        addSingleItem(item.get(), weight)
//    }

    fun addSingleItem(
        item: Item,
        weight: Int,
    ) {
        builder.withPool(singleItem(item, weight))
    }

    fun addSingleItem(
        item: ItemRegistryType,
        rolls: Float = 1f,
        bonus: Float = 0f,
        weight: Int,
        quality: Int = 0,
        block: LootPool.Builder.() -> Unit = {}
    ) {
        builder.withPool(
            singleItem(item.get(), rolls, bonus, weight, quality)
                .apply(block)
        )
    }

    class MultiItemsBuilder() {
        val entries = mutableSetOf<ItemEntry>()

        fun withWeight(weight: Int, vararg items: ItemRegistryType) {
            items.forEach { it weighted weight }
        }

        infix fun ItemRegistryType.weighted(weight: Int): ItemEntry {
            return ItemEntry(this.get(), weight).also { entries += it }
        }

        infix fun Item.weighted(weight: Int): ItemEntry {
            return ItemEntry(this, weight).also { entries += it }
        }

//        infix fun ItemEntry.withQuality(quality: Int): ItemEntry {
//            return ItemEntry(this.item, this.weight, quality).also {
//                entries -= this
//                entries += it
//            }
//        }

        infix fun ItemEntry.withCount(count: Int) {
            setCount(count)
        }

        infix fun ItemEntry.withCount(range: IntRange) {
            setCountBetween(range.first, range.last)
        }
    }

    fun addMultiItems(rolls: Float, bonus: Float, block: MultiItemsBuilder.() -> Unit) {
        builder.withPool(multiItems(rolls, bonus, *MultiItemsBuilder().apply(block).entries.toTypedArray<ItemEntry>()))
    }

    fun addMultiItems(
        rolls: Float,
        bonus: Float,
        block: MultiItemsBuilder.() -> Unit,
        poolModifier: LootPool.Builder.() -> Unit
    ) {
        builder.withPool(
            multiItems(rolls, bonus, *MultiItemsBuilder().apply(block).entries.toTypedArray<ItemEntry>()).apply(
                poolModifier
            )
        )
    }

}

private fun buildTable(key: ResourceKey<LootTable>, block: LootTableBuilder.() -> Unit): LootTableBuilder {
    return LootTableBuilder(key, LootTable.lootTable()).apply(block)
}

class ModCustomLootProvider() : LootTableSubProvider {
    override fun generate(output: BiConsumer<ResourceKey<LootTable>, LootTable.Builder>) {

        output += buildTable(chests("ancient_cpu")) {
            addSingleItem(ANCIENT_CPU, 1f, 1f, 1, 1) {
                `when` { LootItemRandomChanceCondition.randomChance(0.4f).build() }
            }
        }

        output += buildTable(chests("blue_print_common")) {
            addMultiItems(1f, 0f) {
                withWeight(
                    50,
                    TASER_BLUEPRINT,
                    GLOCK_17_BLUEPRINT,
                    MP_443_BLUEPRINT,
                    M_1911_BLUEPRINT,
                    MARLIN_BLUEPRINT,
                )

                withWeight(
                    15,
                    GLOCK_18_BLUEPRINT,
                    M_79_BLUEPRINT,
                    M_4_BLUEPRINT,
                    SKS_BLUEPRINT,
                    K_98_BLUEPRINT,
                    MOSIN_NAGANT_BLUEPRINT,
                    AK_47_BLUEPRINT,
                    M_870_BLUEPRINT,
                    HK_416_BLUEPRINT,
                    AK_12_BLUEPRINT,
                    QBZ_95_BLUEPRINT,
                    RPG_BLUEPRINT,
                    M_2_HB_BLUEPRINT,
                    MP_5_BLUEPRINT,
                    HUNTING_RIFLE_BLUEPRINT,
                )

                withWeight(
                    1,
                    TRACHELIUM_BLUEPRINT,
                    SENTINEL_BLUEPRINT,
                    BOCEK_BLUEPRINT,
                    RPK_BLUEPRINT,
                    VECTOR_BLUEPRINT,
                    MK_14_BLUEPRINT,
                    M_60_BLUEPRINT,
                    SVD_BLUEPRINT,
                    M_98B_BLUEPRINT,
                    AWM_BLUEPRINT,
                    DEVOTION_BLUEPRINT,
                    INSIDIOUS_BLUEPRINT,
                    QBZ_191_BLUEPRINT,
                    IGLA_BLUEPRINT,
                )
            }

            addMultiItems(2f, 0f) {
                HANDGUN_AMMO_BOX weighted 12 withCount 1..2
                RIFLE_AMMO_BOX weighted 20 withCount 1..2
                SNIPER_AMMO_BOX weighted 10 withCount 1..2
                SHOTGUN_AMMO_BOX weighted 17 withCount 1..2
                GRENADE_40MM weighted 6 withCount 1..3
                RPG_ROCKET_TBG weighted 2 withCount 1..2
                RPG_ROCKET_STANDARD weighted 2 withCount 1..2
                MORTAR_SHELL weighted 6 withCount 1..4
                CLAYMORE_MINE weighted 3 withCount 1..3
                C4_BOMB weighted 1
            }
        }

        output += buildTable(chests("blue_print_rare")) {
            addMultiItems(1f, 0f) {
                TASER_BLUEPRINT weighted 10
                GLOCK_17_BLUEPRINT weighted 10
                MP_443_BLUEPRINT weighted 10
                M_1911_BLUEPRINT weighted 10
                MARLIN_BLUEPRINT weighted 10

                GLOCK_18_BLUEPRINT weighted 30
                M_79_BLUEPRINT weighted 30
                M_4_BLUEPRINT weighted 30
                SKS_BLUEPRINT weighted 30
                K_98_BLUEPRINT weighted 30
                MOSIN_NAGANT_BLUEPRINT weighted 30
                AK_47_BLUEPRINT weighted 30
                M_870_BLUEPRINT weighted 30
                HK_416_BLUEPRINT weighted 30
                AK_12_BLUEPRINT weighted 30
                QBZ_95_BLUEPRINT weighted 30
                RPG_BLUEPRINT weighted 30
                M_2_HB_BLUEPRINT weighted 30
                HUNTING_RIFLE_BLUEPRINT weighted 30

                TRACHELIUM_BLUEPRINT weighted 10
                SENTINEL_BLUEPRINT weighted 10
                BOCEK_BLUEPRINT weighted 10
                RPK_BLUEPRINT weighted 10
                VECTOR_BLUEPRINT weighted 10
                MK_14_BLUEPRINT weighted 10
                M_60_BLUEPRINT weighted 10
                SVD_BLUEPRINT weighted 10
                M_98B_BLUEPRINT weighted 10
                AWM_BLUEPRINT weighted 10
                DEVOTION_BLUEPRINT weighted 10
                INSIDIOUS_BLUEPRINT weighted 10
                QBZ_191_BLUEPRINT weighted 10
                IGLA_BLUEPRINT weighted 7

                AA_12_BLUEPRINT weighted 3
                NTW_20_BLUEPRINT weighted 3
                MINIGUN_BLUEPRINT weighted 3
                JAVELIN_BLUEPRINT weighted 3
                SECONDARY_CATACLYSM_BLUEPRINT weighted 3
                AURELIA_SCEPTRE_BLUEPRINT weighted 2
                QL_1031_BLUEPRINT weighted 2
                MK_42_BLUEPRINT weighted 3
                MLE_1934_BLUEPRINT weighted 2
                HPJ_11_BLUEPRINT weighted 2
                BL_132_BLUEPRINT weighted 2
                ANNIHILATOR_BLUEPRINT weighted 1
            }

            addMultiItems(2f, 0f) {
                HANDGUN_AMMO_BOX weighted 12 withCount 1..3
                RIFLE_AMMO_BOX weighted 20 withCount 1..3
                SNIPER_AMMO_BOX weighted 10 withCount 1..3
                SHOTGUN_AMMO_BOX weighted 17 withCount 1..3
                GRENADE_40MM weighted 6 withCount 2..6
                RPG_ROCKET_TBG weighted 2 withCount 2..4
                RPG_ROCKET_STANDARD weighted 2 withCount 2..4
                MORTAR_SHELL weighted 6 withCount 2..8
                CLAYMORE_MINE weighted 3 withCount 2..6
                C4_BOMB weighted 1 withCount 1..2
            }
        }

        output += buildTable(chests("blue_print_epic")) {
            addMultiItems(1f, 0f) {
                TRACHELIUM_BLUEPRINT weighted 10
                SENTINEL_BLUEPRINT weighted 10
                BOCEK_BLUEPRINT weighted 10
                RPK_BLUEPRINT weighted 10
                VECTOR_BLUEPRINT weighted 10
                MK_14_BLUEPRINT weighted 10
                M_60_BLUEPRINT weighted 10
                SVD_BLUEPRINT weighted 10
                M_98B_BLUEPRINT weighted 10
                AWM_BLUEPRINT weighted 10
                DEVOTION_BLUEPRINT weighted 10
                INSIDIOUS_BLUEPRINT weighted 10
                QBZ_191_BLUEPRINT weighted 10
                IGLA_BLUEPRINT weighted 7

                AA_12_BLUEPRINT weighted 20
                NTW_20_BLUEPRINT weighted 20
                MINIGUN_BLUEPRINT weighted 20
                JAVELIN_BLUEPRINT weighted 15
                SECONDARY_CATACLYSM_BLUEPRINT weighted 15
                AURELIA_SCEPTRE_BLUEPRINT weighted 10
                QL_1031_BLUEPRINT weighted 10
                MK_42_BLUEPRINT weighted 10
                MLE_1934_BLUEPRINT weighted 10
                BL_132_BLUEPRINT weighted 7
                HPJ_11_BLUEPRINT weighted 5
                ANNIHILATOR_BLUEPRINT weighted 5
            }

            addMultiItems(2f, 0f) {
                HANDGUN_AMMO_BOX weighted 12 withCount 2..4
                RIFLE_AMMO_BOX weighted 20 withCount 2..4
                SNIPER_AMMO_BOX weighted 10 withCount 2..4
                SHOTGUN_AMMO_BOX weighted 17 withCount 2..4
                HEAVY_AMMO weighted 10 withCount 10..24
                GRENADE_40MM weighted 6 withCount 4..12
                RPG_ROCKET_TBG weighted 2 withCount 4..8
                RPG_ROCKET_STANDARD weighted 2 withCount 4..8
                MORTAR_SHELL weighted 6 withCount 4..8
                CLAYMORE_MINE weighted 3 withCount 4..12
                C4_BOMB weighted 1 withCount 2..4
                JAVELIN_MISSILE weighted 1 withCount 1..2
            }
        }

        output += buildTable(containers("blueprints")) {
            addMultiItems(1f, 0f) {
                GLOCK_17_BLUEPRINT weighted 60
                MP_443_BLUEPRINT weighted 60
                TASER_BLUEPRINT weighted 60
                MARLIN_BLUEPRINT weighted 60
                M_1911_BLUEPRINT weighted 60

                GLOCK_18_BLUEPRINT weighted 42
                M_79_BLUEPRINT weighted 42
                M_4_BLUEPRINT weighted 42
                SKS_BLUEPRINT weighted 42
                M_870_BLUEPRINT weighted 42
                AK_47_BLUEPRINT weighted 42
                K_98_BLUEPRINT weighted 42
                MOSIN_NAGANT_BLUEPRINT weighted 42
                HK_416_BLUEPRINT weighted 42
                AK_12_BLUEPRINT weighted 42
                QBZ_95_BLUEPRINT weighted 42
                RPG_BLUEPRINT weighted 42
                HUNTING_RIFLE_BLUEPRINT weighted 42
                M_2_HB_BLUEPRINT weighted 42

                TRACHELIUM_BLUEPRINT weighted 15
                SENTINEL_BLUEPRINT weighted 15
                BOCEK_BLUEPRINT weighted 15
                RPK_BLUEPRINT weighted 15
                VECTOR_BLUEPRINT weighted 15
                MK_14_BLUEPRINT weighted 15
                M_60_BLUEPRINT weighted 15
                SVD_BLUEPRINT weighted 15
                M_98B_BLUEPRINT weighted 15
                AWM_BLUEPRINT weighted 15
                DEVOTION_BLUEPRINT weighted 15
                INSIDIOUS_BLUEPRINT weighted 15
                QBZ_191_BLUEPRINT weighted 15
                IGLA_BLUEPRINT weighted 10

                AA_12_BLUEPRINT weighted 5
                NTW_20_BLUEPRINT weighted 5
                MINIGUN_BLUEPRINT weighted 5
                JAVELIN_BLUEPRINT weighted 5
                SECONDARY_CATACLYSM_BLUEPRINT weighted 5
                AURELIA_SCEPTRE_BLUEPRINT weighted 5
                QL_1031_BLUEPRINT weighted 5
            }
        }

        output += buildTable(containers("common")) {
            addMultiItems(1f, 0f, {
                EPIC_MATERIAL_PACK weighted 2
                CEMENTED_CARBIDE_BLOCK weighted 2
                Items.EXPERIENCE_BOTTLE weighted 2 withCount 4

                RARE_MATERIAL_PACK weighted 4 withCount 2
                COMMON_MATERIAL_PACK weighted 6 withCount 3
                STEEL_BLOCK weighted 14
                Items.GOLD_BLOCK weighted 20
                HANDGUN_AMMO weighted 6 withCount 64
                RIFLE_AMMO weighted 6 withCount 64
                SHOTGUN_AMMO weighted 6 withCount 32
                SNIPER_AMMO weighted 6 withCount 32
                HEAVY_AMMO weighted 6 withCount 16
                Items.COAL_BLOCK weighted 30 withCount 9
            }, {
                add(NestedLootTable.lootTableReference(special("common/flags")).setWeight(40))
                add(NestedLootTable.lootTableReference(special("common/blueprints")).setWeight(50))
            })

            output += buildTable(special("common/flags")) {
                addSingleItem(Items.RED_BANNER, 1)
                addSingleItem(Items.ORANGE_BANNER, 1)
                addSingleItem(Items.YELLOW_BANNER, 1)
                addSingleItem(Items.GREEN_BANNER, 1)
                addSingleItem(Items.CYAN_BANNER, 1)
                addSingleItem(Items.BLUE_BANNER, 1)
                addSingleItem(Items.PURPLE_BANNER, 1)
                addSingleItem(Items.PINK_BANNER, 1)

                output += buildTable(special("common/blueprints")) {
                    addMultiItems(1f, 0f) {
                        withWeight(
                            4,
                            GLOCK_17_BLUEPRINT,
                            MP_443_BLUEPRINT,
                            M_1911_BLUEPRINT,
                            MARLIN_BLUEPRINT,
                            TASER_BLUEPRINT,
                        )

                        withWeight(
                            2,
                            GLOCK_18_BLUEPRINT,
                            AK_47_BLUEPRINT,
                            QBZ_95_BLUEPRINT,
                            SKS_BLUEPRINT,
                            MOSIN_NAGANT_BLUEPRINT,
                            M_870_BLUEPRINT,
                            M_79_BLUEPRINT,

                            BOCEK_BLUEPRINT,
                            TRACHELIUM_BLUEPRINT,
                            VECTOR_BLUEPRINT,
                            DEVOTION_BLUEPRINT,
                            M_98B_BLUEPRINT,
                            AWM_BLUEPRINT,
                        )

                        withWeight(
                            1,
                            AA_12_BLUEPRINT,
                            NTW_20_BLUEPRINT,
                            MINIGUN_BLUEPRINT,
                            JAVELIN_BLUEPRINT,

                            MK_42_BLUEPRINT,
                            MLE_1934_BLUEPRINT,
                        )
                    }
                }
            }
        }
    }
}

private class ItemEntry @JvmOverloads constructor(
    var item: ItemLike,
    var weight: Int,
    var quality: Int = 0
) {
    var conditions: MutableList<LootItemCondition.Builder> = ArrayList()
    var functions: MutableList<LootItemFunction.Builder> = ArrayList()

//    fun condition(condition: LootItemCondition.Builder): ItemEntry {
//        this.conditions.add(condition)
//        return this
//    }

    fun function(function: LootItemFunction.Builder): ItemEntry {
        this.functions.add(function)
        return this
    }

    fun setCountBetween(min: Int, max: Int): ItemEntry {
        return this.function(
            SetItemCountFunction.setCount(
                UniformGenerator.between(min.toFloat(), max.toFloat())
            )
        )
    }

    fun setCount(count: Int): ItemEntry {
        return this.function(SetItemCountFunction.setCount(ConstantValue.exactly(count.toFloat())))
    }
}