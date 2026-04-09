package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.init.ModBlocks
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.neoforged.neoforge.client.model.generators.BlockStateProvider
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.registries.DeferredHolder

class ModBlockStateProvider(output: PackOutput, exFileHelper: ExistingFileHelper) :
    BlockStateProvider(output, Mod.MODID, exFileHelper) {
    override fun registerStatesAndModels() {
        horizontalBlock(ModBlocks.BARBED_WIRE.get(), UncheckedModelFile(modLoc("block/barbed_wire")))
        horizontalBlock(ModBlocks.JUMP_PAD.get(), UncheckedModelFile(modLoc("block/jump_pad")))
        horizontalBlock(ModBlocks.REFORGING_TABLE.get(), UncheckedModelFile(modLoc("block/reforging_table")))
        horizontalBlock(ModBlocks.CONTAINER.get(), UncheckedModelFile(modLoc("block/container")))
        horizontalBlock(ModBlocks.SMALL_CONTAINER.get(), UncheckedModelFile(modLoc("block/small_container")))
        horizontalBlock(ModBlocks.LUCKY_CONTAINER.get(), UncheckedModelFile(modLoc("block/container")))
        horizontalBlock(
            ModBlocks.CHARGING_STATION.get(), models().cube(
                "charging_station",
                modLoc("block/charging_station_bottom"),
                modLoc("block/charging_station_top"),
                modLoc("block/charging_station_front"),
                modLoc("block/charging_station_side"),
                modLoc("block/charging_station_side"),
                modLoc("block/charging_station_side")
            ).texture("particle", modLoc("block/charging_station_front"))
        )
        horizontalBlock(
            ModBlocks.CREATIVE_CHARGING_STATION.get(), models().cube(
                "creative_charging_station",
                modLoc("block/creative_charging_station_bottom"),
                modLoc("block/creative_charging_station_top"),
                modLoc("block/creative_charging_station_front"),
                modLoc("block/creative_charging_station_side"),
                modLoc("block/creative_charging_station_side"),
                modLoc("block/creative_charging_station_side")
            ).texture("particle", modLoc("block/creative_charging_station_front"))
        )
        horizontalBlock(
            ModBlocks.VEHICLE_DEPLOYER.get(), models().cubeBottomTop(
                "vehicle_deployer", modLoc("block/vehicle_deployer_side"),
                modLoc("block/vehicle_deployer_bottom"), modLoc("block/vehicle_deployer_top")
            ).texture("particle", modLoc("block/vehicle_deployer_bottom"))
        )
        horizontalBlock(
            ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(),
            UncheckedModelFile(modLoc("block/vehicle_assembling_table"))
        )
        horizontalBlock(
            ModBlocks.BLUEPRINT_RESEARCH_TABLE.get(),
            UncheckedModelFile(modLoc("block/blueprint_research_table"))
        )
        simpleBlock(
            ModBlocks.BIOGAS_GENERATOR.get(), models().cubeBottomTop(
                "biogas_generator",
                modLoc("block/biogas_generator_side"),
                modLoc("block/biogas_generator_bottom"),
                modLoc("block/biogas_generator_top")
            ).texture("particle", modLoc("block/biogas_generator_side"))
        )

        horizontalBlock(
            ModBlocks.AIRCRAFT_CATAPULT.get(), models().cube(
                "aircraft_catapult",
                modLoc("block/vehicle_deployer_bottom"),
                modLoc("block/aircraft_catapult_top"),
                modLoc("block/aircraft_catapult_side"),
                modLoc("block/aircraft_catapult_side"),
                modLoc("block/aircraft_catapult_side2"),
                modLoc("block/aircraft_catapult_side2")
            ).texture("particle", modLoc("block/aircraft_catapult_top"))
        )

        directionalBlock(
            ModBlocks.SUPERB_ITEM_INTERFACE.get(), models().cubeBottomTop(
                "superb_item_interface",
                modLoc("block/superb_item_interface_side"),
                modLoc("block/superb_item_interface_bottom"),
                modLoc("block/superb_item_interface_top")
            ).texture("particle", modLoc("block/superb_item_interface_bottom"))
        )

        directionalBlock(
            ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.get(), models().cubeBottomTop(
                "creative_superb_item_interface",
                modLoc("block/creative_superb_item_interface_side"),
                modLoc("block/creative_superb_item_interface_bottom"),
                modLoc("block/creative_superb_item_interface_top")
            ).texture("particle", modLoc("block/creative_superb_item_interface_bottom"))
        )

        blockWithItem(ModBlocks.GALENA_ORE)
        blockWithItem(ModBlocks.DEEPSLATE_GALENA_ORE)
        blockWithItem(ModBlocks.SCHEELITE_ORE)
        blockWithItem(ModBlocks.DEEPSLATE_SCHEELITE_ORE)
        blockWithItem(ModBlocks.LEAD_BLOCK)
        blockWithItem(ModBlocks.STEEL_BLOCK)
        blockWithItem(ModBlocks.TUNGSTEN_BLOCK)
        blockWithItem(ModBlocks.CEMENTED_CARBIDE_BLOCK)
        blockWithItem(ModBlocks.SILVER_ORE)
        blockWithItem(ModBlocks.DEEPSLATE_SILVER_ORE)
        blockWithItem(ModBlocks.SILVER_BLOCK)
        blockWithItem(ModBlocks.RAW_GALENA_BLOCK)
        blockWithItem(ModBlocks.RAW_SCHEELITE_BLOCK)
        blockWithItem(ModBlocks.RAW_SILVER_BLOCK)

        simpleBlock(ModBlocks.FUMO_25.get(), UncheckedModelFile(modLoc("block/fumo_25")))
    }

    private fun name(block: Block): String {
        return key(block).getPath()
    }

    private fun key(block: Block): ResourceLocation {
        return BuiltInRegistries.BLOCK.getKey(block)
    }

    private fun blockItem(blockRegistryObject: DeferredHolder<Block, out Block>) {
        simpleBlockItem(
            blockRegistryObject.get(), UncheckedModelFile(
                Mod.MODID + ":block/" + BuiltInRegistries.BLOCK.getKey(blockRegistryObject.get()).path
            )
        )
    }

    private fun blockWithItem(blockRegistryObject: DeferredHolder<Block, out Block>) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()))
    }
}
