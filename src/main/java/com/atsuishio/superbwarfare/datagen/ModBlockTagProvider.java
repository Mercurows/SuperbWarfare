package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Mod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.GALENA_ORE.get(), ModBlocks.SCHEELITE_ORE.get(),
                ModBlocks.DEEPSLATE_GALENA_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(), ModBlocks.DRAGON_TEETH.get(),
                ModBlocks.SILVER_ORE.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get());

        this.tag(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.BARBED_WIRE.get());
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.GALENA_ORE.get(), ModBlocks.SCHEELITE_ORE.get(),
                ModBlocks.DEEPSLATE_GALENA_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(), ModBlocks.DRAGON_TEETH.get(),
                ModBlocks.REFORGING_TABLE.get(), ModBlocks.LEAD_BLOCK.get(), ModBlocks.STEEL_BLOCK.get(), ModBlocks.TUNGSTEN_BLOCK.get(),
                ModBlocks.CEMENTED_CARBIDE_BLOCK.get(), ModBlocks.SILVER_ORE.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get(),
                ModBlocks.SILVER_BLOCK.get(), ModBlocks.JUMP_PAD.get(), ModBlocks.CONTAINER.get(), ModBlocks.CHARGING_STATION.get(),
                ModBlocks.FUMO_25.get(), ModBlocks.SMALL_CONTAINER.get(), ModBlocks.VEHICLE_DEPLOYER.get(), ModBlocks.AIRCRAFT_CATAPULT.get());
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL).add(ModBlocks.SANDBAG.get());

        this.tag(ModTags.Blocks.SOFT_COLLISION)
                .addTags(BlockTags.FENCES, BlockTags.FENCE_GATES, BlockTags.DOORS, BlockTags.TRAPDOORS, BlockTags.WALLS, BlockTags.WOOL,
                        BlockTags.STAIRS, BlockTags.SLABS, Tags.Blocks.GLASS_PANES)
                .add(Blocks.BAMBOO, Blocks.MELON, Blocks.PUMPKIN, Blocks.HAY_BLOCK, Blocks.BELL, Blocks.CHAIN, Blocks.SNOW_BLOCK,
                        Blocks.MUSHROOM_STEM, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK);

        this.tag(ModTags.Blocks.HARD_COLLISION)
                .addTags(BlockTags.LOGS, BlockTags.PLANKS, Tags.Blocks.GLASS)
                .add(Blocks.ICE, Blocks.FROSTED_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);

        this.tag(Tags.Blocks.ORES).addTags(forgeTag("ores/lead"), forgeTag("ores/tungsten"), forgeTag("ores/silver"));
        this.tag(forgeTag("ores/lead")).add(ModBlocks.GALENA_ORE.get(), ModBlocks.DEEPSLATE_GALENA_ORE.get());
        this.tag(forgeTag("ores/tungsten")).add(ModBlocks.SCHEELITE_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get());
        this.tag(forgeTag("ores/silver")).add(ModBlocks.SILVER_ORE.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get());

        this.tag(Tags.Blocks.ORES_IN_GROUND_STONE).add(ModBlocks.GALENA_ORE.get(), ModBlocks.SCHEELITE_ORE.get(), ModBlocks.SILVER_ORE.get());
        this.tag(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE).add(ModBlocks.DEEPSLATE_GALENA_ORE.get(), ModBlocks.DEEPSLATE_SCHEELITE_ORE.get(), ModBlocks.DEEPSLATE_SILVER_ORE.get());
    }

    public static TagKey<Block> forgeTag(String name) {
        return BlockTags.create(new ResourceLocation("forge", name));
    }
}
