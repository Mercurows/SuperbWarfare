package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.data.loot.EntityLootSubProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import java.util.stream.Stream

class ModEntityLootProvider : EntityLootSubProvider(FeatureFlags.REGISTRY.allFlags()) {
    override fun generate() {
        this.add(
            ModEntities.STEEL_COIL.get(),
            LootTable.lootTable().withPool(
                LootPool.lootPool().setRolls(ConstantValue.exactly(1f))
                    .add(
                        LootItem.lootTableItem(ModItems.STEEL_BLOCK.get())
                            .apply(
                                SetItemCountFunction.setCount(ConstantValue.exactly(3f))
                            )
                    )
            ).withPool(
                LootPool.lootPool().setRolls(ConstantValue.exactly(1f))
                    .add(
                        LootItem.lootTableItem(ModItems.STEEL_BLOCK.get())
                            .`when`(LootItemRandomChanceCondition.randomChance(0.5f))
                    )
            )
        )
        this.add(ModEntities.TARGET.get(), LootTable.lootTable())
        this.add(ModEntities.DPS_GENERATOR.get(), LootTable.lootTable())
        this.add(
            ModEntities.SENPAI.get(),
            LootTable.lootTable().withPool(
                LootPool.lootPool().setRolls(ConstantValue.exactly(1f))
                    .add(LootItem.lootTableItem(Items.APPLE).setWeight(80))
                    .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(19))
                    .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1))
            )
        )
    }

    override fun getKnownEntityTypes(): Stream<EntityType<*>> {
        return ModEntities.REGISTRY.entries.stream().map { it.get() }
    }
}