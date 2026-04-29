package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.block.BlueprintResearchTableBlock
import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock
import com.atsuishio.superbwarfare.block.property.BlockPart
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.data.loot.BlockLootSubProvider
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue

class ModBlockLootProvider(provider: HolderLookup.Provider) :
    BlockLootSubProvider(mutableSetOf<Item>(), FeatureFlags.REGISTRY.allFlags(), provider) {
    override fun generate() {
        this.dropSelf(ModBlocks.SANDBAG.get())
        this.dropSelf(ModBlocks.BARBED_WIRE.get())
        this.dropSelf(ModBlocks.JUMP_PAD.get())
        this.dropSelf(ModBlocks.DRAGON_TEETH.get())
        this.dropSelf(ModBlocks.REFORGING_TABLE.get())
        this.dropSelf(ModBlocks.LEAD_BLOCK.get())
        this.dropSelf(ModBlocks.STEEL_BLOCK.get())
        this.dropSelf(ModBlocks.TUNGSTEN_BLOCK.get())
        this.dropSelf(ModBlocks.CEMENTED_CARBIDE_BLOCK.get())
        this.dropSelf(ModBlocks.SILVER_BLOCK.get())
        this.dropSelf(ModBlocks.CREATIVE_CHARGING_STATION.get())
        this.dropSelf(ModBlocks.FUMO_25.get())
        this.dropSelf(ModBlocks.VEHICLE_DEPLOYER.get())
        this.dropSelf(ModBlocks.AIRCRAFT_CATAPULT.get())
        this.dropSelf(ModBlocks.SUPERB_ITEM_INTERFACE.get())
        this.dropSelf(ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.get())
        this.dropSelf(ModBlocks.RAW_GALENA_BLOCK.get())
        this.dropSelf(ModBlocks.RAW_SCHEELITE_BLOCK.get())
        this.dropSelf(ModBlocks.RAW_SILVER_BLOCK.get())
        this.add(
            ModBlocks.BLUEPRINT_RESEARCH_TABLE.get(),
            this.applyExplosionDecay(
                ModBlocks.BLUEPRINT_RESEARCH_TABLE.get(), LootTable.lootTable().withPool(
                    LootPool.lootPool().add(
                        LootItem.lootTableItem(ModBlocks.BLUEPRINT_RESEARCH_TABLE.get()).`when`(
                            LootItemBlockStatePropertyCondition.hasBlockStateProperties(
                                ModBlocks.BLUEPRINT_RESEARCH_TABLE.get()
                            ).setProperties(
                                StatePropertiesPredicate.Builder.properties()
                                    .hasProperty(BlueprintResearchTableBlock.PART, BedPart.FOOT)
                            )
                        ).otherwise(LootItem.lootTableItem(Blocks.AIR))
                    )
                )
            )
        )
        this.add(
            ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(),
            this.applyExplosionDecay(
                ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(), LootTable.lootTable().withPool(
                    LootPool.lootPool().add(
                        LootItem.lootTableItem(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get()).`when`(
                            LootItemBlockStatePropertyCondition.hasBlockStateProperties(
                                ModBlocks.VEHICLE_ASSEMBLING_TABLE.get()
                            ).setProperties(
                                StatePropertiesPredicate.Builder.properties()
                                    .hasProperty(VehicleAssemblingTableBlock.BLOCK_PART, BlockPart.FLB)
                            )
                        ).otherwise(LootItem.lootTableItem(Blocks.AIR))
                    )
                )
            )
        )
        this.dropSelf(ModBlocks.BIOGAS_GENERATOR.get())

        this.add(
            ModBlocks.CHARGING_STATION.get(), createCopyComponentsDrops(
                ModBlocks.CHARGING_STATION.get(),
                listOf(ModDataComponents.ENERGY.get())
            )
        )
        this.add(ModBlocks.GALENA_ORE.get(), this.createOreDrop(ModBlocks.GALENA_ORE.get(), ModItems.GALENA.get()))
        this.add(
            ModBlocks.SCHEELITE_ORE.get(),
            this.createOreDrop(ModBlocks.SCHEELITE_ORE.get(), ModItems.SCHEELITE.get())
        )
        this.add(ModBlocks.SILVER_ORE.get(), this.createOreDrop(ModBlocks.SILVER_ORE.get(), ModItems.RAW_SILVER.get()))
        this.add(
            ModBlocks.DEEPSLATE_GALENA_ORE.get(),
            this.createOreDrop(ModBlocks.DEEPSLATE_GALENA_ORE.get(), ModItems.GALENA.get())
        )
        this.add(
            ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(),
            this.createOreDrop(ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(), ModItems.SCHEELITE.get())
        )
        this.add(
            ModBlocks.DEEPSLATE_SILVER_ORE.get(),
            this.createOreDrop(ModBlocks.DEEPSLATE_SILVER_ORE.get(), ModItems.RAW_SILVER.get())
        )

        this.add(
            ModBlocks.CONTAINER.get(), LootTable.lootTable().withPool(
                this.applyExplosionCondition(
                    ModBlocks.CONTAINER.get(),
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1f))
                        .add(LootItem.lootTableItem(ModBlocks.CONTAINER.get()))
                        .apply(
                            CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(DataComponents.BLOCK_ENTITY_DATA)
                        )
                )
            )
        )
        this.add(
            ModBlocks.SMALL_CONTAINER.get(), LootTable.lootTable().withPool(
                this.applyExplosionCondition(
                    ModBlocks.SMALL_CONTAINER.get(),
                    LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1f))
                        .add(LootItem.lootTableItem(ModBlocks.SMALL_CONTAINER.get()))
                        .apply(
                            CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(DataComponents.CONTAINER_LOOT)
                        )
                )
            )
        )
        this.add(
            ModBlocks.LUCKY_CONTAINER.get(), LootTable.lootTable().withPool(
                this.applyExplosionCondition(
                    ModBlocks.LUCKY_CONTAINER.get(),
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1f))
                        .add(LootItem.lootTableItem(ModBlocks.LUCKY_CONTAINER.get()))
                        .apply(
                            CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(DataComponents.BLOCK_ENTITY_DATA)
                        )
                )
            )
        )
    }

    override fun getKnownBlocks(): Iterable<Block> {
        return Iterable { ModBlocks.REGISTRY.getEntries().stream().map { it.get() }.iterator() }
    }

    fun createCopyComponentsDrops(
        pBlock: Block,
        components: List<DataComponentType<*>>
    ): LootTable.Builder {
        val pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1f)).add(LootItem.lootTableItem(pBlock))
        if (!components.isEmpty()) {
            val copy = CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
            for (type in components) {
                copy.include(type)
            }
            pool.apply(copy)
        }
        return LootTable.lootTable().withPool(this.applyExplosionCondition(pBlock, pool))
    }
}
