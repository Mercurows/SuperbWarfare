package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.init.ModTags.commonBlockTag
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import java.util.concurrent.CompletableFuture

class ModBlockTagProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper
) : BlockTagsProvider(output, lookupProvider, Mod.MODID, existingFileHelper) {
    override fun addTags(pProvider: HolderLookup.Provider) {
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(
            ModBlocks.GALENA_ORE.get(), ModBlocks.SCHEELITE_ORE.get(),
            ModBlocks.DEEPSLATE_GALENA_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(),
            ModBlocks.SILVER_ORE.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get(),
            ModBlocks.RAW_GALENA_BLOCK.get(), ModBlocks.RAW_SCHEELITE_BLOCK.get(),
            ModBlocks.RAW_SILVER_BLOCK.get(), ModBlocks.DRAGON_TEETH.get()
        )

        this.tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.BARBED_WIRE.get())
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            ModBlocks.GALENA_ORE.get(),
            ModBlocks.SCHEELITE_ORE.get(),
            ModBlocks.DEEPSLATE_GALENA_ORE.get(),
            ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(),
            ModBlocks.DRAGON_TEETH.get(),
            ModBlocks.REFORGING_TABLE.get(),
            ModBlocks.LEAD_BLOCK.get(),
            ModBlocks.STEEL_BLOCK.get(),
            ModBlocks.TUNGSTEN_BLOCK.get(),
            ModBlocks.CEMENTED_CARBIDE_BLOCK.get(),
            ModBlocks.SILVER_ORE.get(),
            ModBlocks.DEEPSLATE_SILVER_ORE.get(),
            ModBlocks.SILVER_BLOCK.get(),
            ModBlocks.JUMP_PAD.get(),
            ModBlocks.CONTAINER.get(),
            ModBlocks.CHARGING_STATION.get(),
            ModBlocks.FUMO_25.get(),
            ModBlocks.SMALL_CONTAINER.get(),
            ModBlocks.VEHICLE_DEPLOYER.get(),
            ModBlocks.AIRCRAFT_CATAPULT.get(),
            ModBlocks.SUPERB_ITEM_INTERFACE.get(),
            ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.get(),
            ModBlocks.LUCKY_CONTAINER.get(),
            ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(),
            ModBlocks.BIOGAS_GENERATOR.get(),
            ModBlocks.BLUEPRINT_RESEARCH_TABLE.get(),
            ModBlocks.RAW_GALENA_BLOCK.get(),
            ModBlocks.RAW_SCHEELITE_BLOCK.get(),
            ModBlocks.RAW_SILVER_BLOCK.get()
        )
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.SANDBAG.get())

        this.tag(ModTags.Blocks.MINEABLE_WITH_MILITARY_SHOVEL)
            .add(Blocks.COBWEB)
            .addTag(BlockTags.MINEABLE_WITH_SHOVEL)
            .addTag(BlockTags.MINEABLE_WITH_AXE)
            .addTag(BlockTags.MINEABLE_WITH_HOE)

        this.tag(ModTags.Blocks.SOFT_COLLISION)
            .addTag(BlockTags.LEAVES)
            .add(Blocks.LILY_PAD, Blocks.COBWEB, Blocks.CACTUS, Blocks.MANGROVE_ROOTS)
        this.tag(ModTags.Blocks.NORMAL_COLLISION)
            .addTags(
                BlockTags.FENCES,
                BlockTags.FENCE_GATES,
                BlockTags.DOORS,
                BlockTags.TRAPDOORS,
                BlockTags.WALLS,
                BlockTags.WOOL,
                BlockTags.STAIRS,
                BlockTags.SLABS,
                Tags.Blocks.GLASS_PANES
            )
            .add(
                Blocks.BAMBOO,
                Blocks.MELON,
                Blocks.PUMPKIN,
                Blocks.HAY_BLOCK,
                Blocks.BELL,
                Blocks.CHAIN,
                Blocks.SNOW_BLOCK,
                Blocks.MUSHROOM_STEM,
                Blocks.BROWN_MUSHROOM_BLOCK,
                Blocks.RED_MUSHROOM_BLOCK
            )
        this.tag(ModTags.Blocks.HARD_COLLISION)
            .addTags(BlockTags.LOGS, BlockTags.PLANKS, Tags.Blocks.GLASS_BLOCKS)
            .add(Blocks.ICE, Blocks.FROSTED_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE)
        this.tag(ModTags.Blocks.BULLET_IGNORE)
            .addTags(
                BlockTags.FENCES,
                BlockTags.FENCE_GATES,
                BlockTags.DOORS,
                BlockTags.TRAPDOORS,
                BlockTags.WALLS,
                BlockTags.LEAVES,
                Tags.Blocks.GLASS_PANES
            )
            .add(Blocks.IRON_BARS, ModBlocks.BARBED_WIRE.get())
        this.tag(ModTags.Blocks.BULLET_CAN_DESTROY)
            .addTags(Tags.Blocks.GLASS_PANES, Tags.Blocks.GLASS_BLOCKS)
        this.tag(ModTags.Blocks.CANNON_SHOT_CAN_DESTROY)
            .addTags(
                ModTags.Blocks.BULLET_CAN_DESTROY, BlockTags.LEAVES, BlockTags.BAMBOO_BLOCKS, BlockTags.WOOL,
                BlockTags.SIGNS, BlockTags.LOGS, BlockTags.PLANKS, BlockTags.SAPLINGS
            )
            .add(Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CHAIN)
        this.tag(ModTags.Blocks.AUTO_LANDING)
            .add(ModBlocks.CHARGING_STATION.get(), ModBlocks.CREATIVE_CHARGING_STATION.get())
        this.tag(ModTags.Blocks.VEHICLE_PASS_THROUGH)
            .addTag(BlockTags.SWORD_EFFICIENT)

        this.tag(Tags.Blocks.ORES).addTags(commonBlockTag("ores/lead"), commonBlockTag("ores/tungsten"), commonBlockTag("ores/silver"))
        this.tag(commonBlockTag("ores/lead")).add(ModBlocks.GALENA_ORE.get(), ModBlocks.DEEPSLATE_GALENA_ORE.get())
        this.tag(commonBlockTag("ores/tungsten")).add(ModBlocks.SCHEELITE_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get())
        this.tag(commonBlockTag("ores/silver")).add(ModBlocks.SILVER_ORE.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get())

        // 这个tag仅用于其他mod配方兼容，自己家配方不用这个
        this.tag(commonBlockTag("ores/scheelite")).add(ModBlocks.SCHEELITE_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get())

        this.tag(commonBlockTag("storage_blocks/raw_lead")).add(ModBlocks.RAW_GALENA_BLOCK.get())
        this.tag(commonBlockTag("storage_blocks/raw_tungsten")).add(ModBlocks.RAW_SCHEELITE_BLOCK.get())
        this.tag(commonBlockTag("storage_blocks/raw_silver")).add(ModBlocks.RAW_SILVER_BLOCK.get())

        this.tag(commonBlockTag("storage_blocks/raw_scheelite")).add(ModBlocks.RAW_SCHEELITE_BLOCK.get())

        this.tag(Tags.Blocks.ORES_IN_GROUND_STONE)
            .add(ModBlocks.GALENA_ORE.get(), ModBlocks.SCHEELITE_ORE.get(), ModBlocks.SILVER_ORE.get())
        this.tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).add(
            ModBlocks.DEEPSLATE_GALENA_ORE.get(),
            ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(),
            ModBlocks.DEEPSLATE_SILVER_ORE.get()
        )
    }
}
