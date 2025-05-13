package com.atsuishio.superbwarfare.datagen;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

@SuppressWarnings({"ConstantConditions", "SameParameterValue"})
public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Mod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(ModBlocks.BARBED_WIRE.get(), new ModelFile.UncheckedModelFile(modLoc("block/barbed_wire")));
        horizontalBlock(ModBlocks.JUMP_PAD.get(), new ModelFile.UncheckedModelFile(modLoc("block/jump_pad")));
        horizontalBlock(ModBlocks.REFORGING_TABLE.get(), new ModelFile.UncheckedModelFile(modLoc("block/reforging_table")));
        horizontalBlock(ModBlocks.CONTAINER.get(), new ModelFile.UncheckedModelFile(modLoc("block/container")));
        horizontalBlock(ModBlocks.SMALL_CONTAINER.get(), new ModelFile.UncheckedModelFile(modLoc("block/small_container")));
        horizontalBlock(ModBlocks.CHARGING_STATION.get(), new ModelFile.UncheckedModelFile(modLoc("block/charging_station")));
        horizontalBlock(ModBlocks.CREATIVE_CHARGING_STATION.get(), new ModelFile.UncheckedModelFile(modLoc("block/creative_charging_station")));
        horizontalBlock(ModBlocks.VEHICLE_DEPLOYER.get(), models().cubeBottomTop("vehicle_deployer", Mod.loc("block/vehicle_deployer_side"),
                        Mod.loc("block/vehicle_deployer_bottom"), Mod.loc("block/vehicle_deployer_top"))
                .texture("particle", Mod.loc("block/vehicle_deployer_bottom")));

        horizontalBlock(ModBlocks.AIRCRAFT_CATAPULT.get(), models().cube("aircraft_catapult",
                        Mod.loc("block/vehicle_deployer_bottom"),
                        Mod.loc("block/aircraft_catapult_top"),
                        Mod.loc("block/aircraft_catapult_side"),
                        Mod.loc("block/aircraft_catapult_side"),
                        Mod.loc("block/aircraft_catapult_side2"),
                        Mod.loc("block/aircraft_catapult_side2"))
                .texture("particle", Mod.loc("block/aircraft_catapult_top")));

        blockWithItem(ModBlocks.GALENA_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_GALENA_ORE);
        blockWithItem(ModBlocks.SCHEELITE_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_SCHEELITE_ORE);
        blockWithItem(ModBlocks.LEAD_BLOCK);
        blockWithItem(ModBlocks.STEEL_BLOCK);
        blockWithItem(ModBlocks.TUNGSTEN_BLOCK);
        blockWithItem(ModBlocks.CEMENTED_CARBIDE_BLOCK);
        blockWithItem(ModBlocks.SILVER_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_SILVER_ORE);
        blockWithItem(ModBlocks.SILVER_BLOCK);

        simpleBlock(ModBlocks.FUMO_25.get(), new ModelFile.UncheckedModelFile(modLoc("block/fumo_25")));
    }

    private String name(Block block) {
        return key(block).getPath();
    }

    private ResourceLocation key(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private void blockItem(DeferredHolder<Block, Block> blockRegistryObject) {
        simpleBlockItem(blockRegistryObject.get(), new ModelFile.UncheckedModelFile(Mod.MODID +
                ":block/" + BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).getPath()));
    }

    private void blockWithItem(DeferredHolder<Block, Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}
