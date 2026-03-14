package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.data.loot.LootTableSubProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer
import net.minecraft.world.level.storage.loot.entries.LootTableReference
import net.minecraft.world.level.storage.loot.functions.LootItemFunction
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import net.minecraftforge.registries.RegistryObject
import java.util.function.BiConsumer

private fun containers(name: String): ResourceLocation {
    return loc("containers/$name")
}

private fun chests(name: String): ResourceLocation {
    return loc("chests/$name")
}

private fun special(name: String): ResourceLocation {
    return loc("special/$name")
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

private typealias ItemRegistryType = RegistryObject<Item>


private operator fun BiConsumer<ResourceLocation, LootTable.Builder>.plusAssign(builder: LootTableBuilder) {
    accept(builder.key, builder.builder)
}

private class LootTableBuilder(val key: ResourceLocation, val builder: LootTable.Builder) {

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

    class MultiItemsBuilder {
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

private fun buildLootTable(key: ResourceLocation, block: LootTableBuilder.() -> Unit): LootTableBuilder {
    return LootTableBuilder(key, LootTable.lootTable()).apply(block)
}

class ModCustomLootProvider : LootTableSubProvider {
    override fun generate(output: BiConsumer<ResourceLocation, LootTable.Builder>) {

        output += buildLootTable(chests("ancient_cpu")) {
            addSingleItem(ModItems.ANCIENT_CPU, 1f, 1f, 1, 1) {
                `when` { LootItemRandomChanceCondition.randomChance(0.4f).build() }
            }
        }

        output += buildLootTable(chests("blue_print_common")) {
            addMultiItems(1f, 0f) {
                withWeight(
                    50,
                    ModItems.TASER_BLUEPRINT,
                    ModItems.GLOCK_17_BLUEPRINT,
                    ModItems.MP_443_BLUEPRINT,
                    ModItems.M_1911_BLUEPRINT,
                    ModItems.MARLIN_BLUEPRINT,
                )

                withWeight(
                    15,
                    ModItems.GLOCK_18_BLUEPRINT,
                    ModItems.M_79_BLUEPRINT,
                    ModItems.M_4_BLUEPRINT,
                    ModItems.SKS_BLUEPRINT,
                    ModItems.K_98_BLUEPRINT,
                    ModItems.MOSIN_NAGANT_BLUEPRINT,
                    ModItems.AK_47_BLUEPRINT,
                    ModItems.M_870_BLUEPRINT,
                    ModItems.HK_416_BLUEPRINT,
                    ModItems.AK_12_BLUEPRINT,
                    ModItems.QBZ_95_BLUEPRINT,
                    ModItems.RPG_BLUEPRINT,
                    ModItems.M_2_HB_BLUEPRINT,
                    ModItems.MP_5_BLUEPRINT,
                    ModItems.HUNTING_RIFLE_BLUEPRINT,
                )

                withWeight(
                    1,
                    ModItems.SENTINEL_BLUEPRINT,
                    ModItems.BOCEK_BLUEPRINT,
                    ModItems.RPK_BLUEPRINT,
                    ModItems.VECTOR_BLUEPRINT,
                    ModItems.MK_14_BLUEPRINT,
                    ModItems.M_60_BLUEPRINT,
                    ModItems.SVD_BLUEPRINT,
                    ModItems.M_98B_BLUEPRINT,
                    ModItems.AWM_BLUEPRINT,
                    ModItems.DEVOTION_BLUEPRINT,
                    ModItems.INSIDIOUS_BLUEPRINT,
                    ModItems.QBZ_191_BLUEPRINT,
                    ModItems.IGLA_BLUEPRINT,
                )
            }

            addMultiItems(2f, 0f) {
                ModItems.HANDGUN_AMMO_BOX weighted 12 withCount 1..2
                ModItems.RIFLE_AMMO_BOX weighted 20 withCount 1..2
                ModItems.SNIPER_AMMO_BOX weighted 10 withCount 1..2
                ModItems.SHOTGUN_AMMO_BOX weighted 17 withCount 1..2
                ModItems.GRENADE_40MM weighted 6 withCount 1..3
                ModItems.RPG_ROCKET_TBG weighted 2 withCount 1..2
                ModItems.RPG_ROCKET_STANDARD weighted 2 withCount 1..2
                ModItems.MORTAR_SHELL weighted 6 withCount 1..4
                ModItems.CLAYMORE_MINE weighted 3 withCount 1..3
                ModItems.C4_BOMB weighted 1
            }
        }

        output += buildLootTable(chests("blue_print_rare")) {
            addMultiItems(1f, 0f) {
                ModItems.TASER_BLUEPRINT weighted 10
                ModItems.GLOCK_17_BLUEPRINT weighted 10
                ModItems.MP_443_BLUEPRINT weighted 10
                ModItems.M_1911_BLUEPRINT weighted 10
                ModItems.MARLIN_BLUEPRINT weighted 10

                ModItems.GLOCK_18_BLUEPRINT weighted 30
                ModItems.M_79_BLUEPRINT weighted 30
                ModItems.M_4_BLUEPRINT weighted 30
                ModItems.SKS_BLUEPRINT weighted 30
                ModItems.K_98_BLUEPRINT weighted 30
                ModItems.MOSIN_NAGANT_BLUEPRINT weighted 30
                ModItems.AK_47_BLUEPRINT weighted 30
                ModItems.M_870_BLUEPRINT weighted 30
                ModItems.HK_416_BLUEPRINT weighted 30
                ModItems.AK_12_BLUEPRINT weighted 30
                ModItems.QBZ_95_BLUEPRINT weighted 30
                ModItems.RPG_BLUEPRINT weighted 30
                ModItems.M_2_HB_BLUEPRINT weighted 30
                ModItems.HUNTING_RIFLE_BLUEPRINT weighted 30

                ModItems.SENTINEL_BLUEPRINT weighted 10
                ModItems.BOCEK_BLUEPRINT weighted 10
                ModItems.RPK_BLUEPRINT weighted 10
                ModItems.VECTOR_BLUEPRINT weighted 10
                ModItems.MK_14_BLUEPRINT weighted 10
                ModItems.M_60_BLUEPRINT weighted 10
                ModItems.SVD_BLUEPRINT weighted 10
                ModItems.M_98B_BLUEPRINT weighted 10
                ModItems.AWM_BLUEPRINT weighted 10
                ModItems.DEVOTION_BLUEPRINT weighted 10
                ModItems.INSIDIOUS_BLUEPRINT weighted 10
                ModItems.QBZ_191_BLUEPRINT weighted 10
                ModItems.IGLA_BLUEPRINT weighted 7

                ModItems.TRACHELIUM_BLUEPRINT weighted 5
                ModItems.SECONDARY_CATACLYSM_BLUEPRINT weighted 5
                ModItems.QL_1031_BLUEPRINT weighted 5

                ModItems.AA_12_BLUEPRINT weighted 3
                ModItems.NTW_20_BLUEPRINT weighted 3
                ModItems.MINIGUN_BLUEPRINT weighted 3
                ModItems.JAVELIN_BLUEPRINT weighted 3
                ModItems.MK_42_BLUEPRINT weighted 3
                ModItems.MLE_1934_BLUEPRINT weighted 2
                ModItems.HPJ_11_BLUEPRINT weighted 2
                ModItems.BL_132_BLUEPRINT weighted 2
                ModItems.ANNIHILATOR_BLUEPRINT weighted 1
            }

            addMultiItems(2f, 0f) {
                ModItems.HANDGUN_AMMO_BOX weighted 12 withCount 1..3
                ModItems.RIFLE_AMMO_BOX weighted 20 withCount 1..3
                ModItems.SNIPER_AMMO_BOX weighted 10 withCount 1..3
                ModItems.SHOTGUN_AMMO_BOX weighted 17 withCount 1..3
                ModItems.GRENADE_40MM weighted 6 withCount 2..6
                ModItems.RPG_ROCKET_TBG weighted 2 withCount 2..4
                ModItems.RPG_ROCKET_STANDARD weighted 2 withCount 2..4
                ModItems.MORTAR_SHELL weighted 6 withCount 2..8
                ModItems.CLAYMORE_MINE weighted 3 withCount 2..6
                ModItems.C4_BOMB weighted 1 withCount 1..2
            }
        }

        output += buildLootTable(chests("blue_print_epic")) {
            addMultiItems(1f, 0f) {
                ModItems.SENTINEL_BLUEPRINT weighted 10
                ModItems.BOCEK_BLUEPRINT weighted 10
                ModItems.RPK_BLUEPRINT weighted 10
                ModItems.VECTOR_BLUEPRINT weighted 10
                ModItems.MK_14_BLUEPRINT weighted 10
                ModItems.M_60_BLUEPRINT weighted 10
                ModItems.SVD_BLUEPRINT weighted 10
                ModItems.M_98B_BLUEPRINT weighted 10
                ModItems.AWM_BLUEPRINT weighted 10
                ModItems.DEVOTION_BLUEPRINT weighted 10
                ModItems.INSIDIOUS_BLUEPRINT weighted 10
                ModItems.QBZ_191_BLUEPRINT weighted 10
                ModItems.IGLA_BLUEPRINT weighted 10

                ModItems.TRACHELIUM_BLUEPRINT weighted 15
                ModItems.SECONDARY_CATACLYSM_BLUEPRINT weighted 15
                ModItems.QL_1031_BLUEPRINT weighted 15

                ModItems.AA_12_BLUEPRINT weighted 20
                ModItems.NTW_20_BLUEPRINT weighted 20
                ModItems.MINIGUN_BLUEPRINT weighted 20
                ModItems.JAVELIN_BLUEPRINT weighted 15
                ModItems.MK_42_BLUEPRINT weighted 10
                ModItems.MLE_1934_BLUEPRINT weighted 10
                ModItems.BL_132_BLUEPRINT weighted 7
                ModItems.HPJ_11_BLUEPRINT weighted 5
                ModItems.ANNIHILATOR_BLUEPRINT weighted 5
            }

            addMultiItems(2f, 0f) {
                ModItems.HANDGUN_AMMO_BOX weighted 12 withCount 2..4
                ModItems.RIFLE_AMMO_BOX weighted 20 withCount 2..4
                ModItems.SNIPER_AMMO_BOX weighted 10 withCount 2..4
                ModItems.SHOTGUN_AMMO_BOX weighted 17 withCount 2..4
                ModItems.HEAVY_AMMO weighted 10 withCount 10..24
                ModItems.GRENADE_40MM weighted 6 withCount 4..12
                ModItems.RPG_ROCKET_TBG weighted 2 withCount 4..8
                ModItems.RPG_ROCKET_STANDARD weighted 2 withCount 4..8
                ModItems.MORTAR_SHELL weighted 6 withCount 4..8
                ModItems.CLAYMORE_MINE weighted 3 withCount 4..12
                ModItems.C4_BOMB weighted 1 withCount 2..4
                ModItems.JAVELIN_MISSILE weighted 1 withCount 1..2
            }
        }

        output += buildLootTable(containers("blueprints")) {
            addMultiItems(1f, 0f) {
                ModItems.GLOCK_17_BLUEPRINT weighted 60
                ModItems.MP_443_BLUEPRINT weighted 60
                ModItems.TASER_BLUEPRINT weighted 60
                ModItems.MARLIN_BLUEPRINT weighted 60
                ModItems.M_1911_BLUEPRINT weighted 60

                ModItems.GLOCK_18_BLUEPRINT weighted 42
                ModItems.M_79_BLUEPRINT weighted 42
                ModItems.M_4_BLUEPRINT weighted 42
                ModItems.SKS_BLUEPRINT weighted 42
                ModItems.M_870_BLUEPRINT weighted 42
                ModItems.AK_47_BLUEPRINT weighted 42
                ModItems.K_98_BLUEPRINT weighted 42
                ModItems.MOSIN_NAGANT_BLUEPRINT weighted 42
                ModItems.HK_416_BLUEPRINT weighted 42
                ModItems.AK_12_BLUEPRINT weighted 42
                ModItems.QBZ_95_BLUEPRINT weighted 42
                ModItems.RPG_BLUEPRINT weighted 42
                ModItems.HUNTING_RIFLE_BLUEPRINT weighted 42
                ModItems.M_2_HB_BLUEPRINT weighted 42

                ModItems.SENTINEL_BLUEPRINT weighted 15
                ModItems.BOCEK_BLUEPRINT weighted 15
                ModItems.RPK_BLUEPRINT weighted 15
                ModItems.VECTOR_BLUEPRINT weighted 15
                ModItems.MK_14_BLUEPRINT weighted 15
                ModItems.M_60_BLUEPRINT weighted 15
                ModItems.SVD_BLUEPRINT weighted 15
                ModItems.M_98B_BLUEPRINT weighted 15
                ModItems.AWM_BLUEPRINT weighted 15
                ModItems.DEVOTION_BLUEPRINT weighted 15
                ModItems.INSIDIOUS_BLUEPRINT weighted 15
                ModItems.QBZ_191_BLUEPRINT weighted 15
                ModItems.IGLA_BLUEPRINT weighted 10

                ModItems.TRACHELIUM_BLUEPRINT weighted 8
                ModItems.SECONDARY_CATACLYSM_BLUEPRINT weighted 8
                ModItems.QL_1031_BLUEPRINT weighted 8

                ModItems.AA_12_BLUEPRINT weighted 5
                ModItems.NTW_20_BLUEPRINT weighted 5
                ModItems.MINIGUN_BLUEPRINT weighted 5
                ModItems.JAVELIN_BLUEPRINT weighted 5
            }
        }

        output += buildLootTable(containers("common")) {
            addMultiItems(1f, 0f, {
                ModItems.EPIC_MATERIAL_PACK weighted 2
                ModItems.CEMENTED_CARBIDE_BLOCK weighted 2
                Items.EXPERIENCE_BOTTLE weighted 2 withCount 4

                ModItems.RARE_MATERIAL_PACK weighted 4 withCount 2
                ModItems.COMMON_MATERIAL_PACK weighted 6 withCount 3
                ModItems.STEEL_BLOCK weighted 14
                Items.GOLD_BLOCK weighted 20
                ModItems.HANDGUN_AMMO weighted 6 withCount 64
                ModItems.RIFLE_AMMO weighted 6 withCount 64
                ModItems.SHOTGUN_AMMO weighted 6 withCount 32
                ModItems.SNIPER_AMMO weighted 6 withCount 32
                ModItems.HEAVY_AMMO weighted 6 withCount 16
                Items.COAL_BLOCK weighted 30 withCount 9
            }, {
                add(LootTableReference.lootTableReference(special("common/flags")).setWeight(40))
                add(LootTableReference.lootTableReference(special("common/blueprints")).setWeight(50))
            })
        }

        output += buildLootTable(special("common/flags")) {
            addSingleItem(Items.RED_BANNER, 1)
            addSingleItem(Items.ORANGE_BANNER, 1)
            addSingleItem(Items.YELLOW_BANNER, 1)
            addSingleItem(Items.GREEN_BANNER, 1)
            addSingleItem(Items.CYAN_BANNER, 1)
            addSingleItem(Items.BLUE_BANNER, 1)
            addSingleItem(Items.PURPLE_BANNER, 1)
            addSingleItem(Items.PINK_BANNER, 1)
        }

        output += buildLootTable(special("common/blueprints")) {
            addMultiItems(1f, 0f) {
                withWeight(
                    4,
                    ModItems.GLOCK_17_BLUEPRINT,
                    ModItems.MP_443_BLUEPRINT,
                    ModItems.M_1911_BLUEPRINT,
                    ModItems.MARLIN_BLUEPRINT,
                    ModItems.TASER_BLUEPRINT,
                )

                withWeight(
                    2,
                    ModItems.GLOCK_18_BLUEPRINT,
                    ModItems.AK_47_BLUEPRINT,
                    ModItems.QBZ_95_BLUEPRINT,
                    ModItems.SKS_BLUEPRINT,
                    ModItems.MOSIN_NAGANT_BLUEPRINT,
                    ModItems.M_870_BLUEPRINT,
                    ModItems.M_79_BLUEPRINT,

                    ModItems.BOCEK_BLUEPRINT,
                    ModItems.TRACHELIUM_BLUEPRINT,
                    ModItems.VECTOR_BLUEPRINT,
                    ModItems.DEVOTION_BLUEPRINT,
                    ModItems.M_98B_BLUEPRINT,
                    ModItems.AWM_BLUEPRINT,
                )

                withWeight(
                    1,
                    ModItems.AA_12_BLUEPRINT,
                    ModItems.NTW_20_BLUEPRINT,
                    ModItems.MINIGUN_BLUEPRINT,
                    ModItems.JAVELIN_BLUEPRINT,

                    ModItems.MK_42_BLUEPRINT,
                    ModItems.MLE_1934_BLUEPRINT,
                )
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