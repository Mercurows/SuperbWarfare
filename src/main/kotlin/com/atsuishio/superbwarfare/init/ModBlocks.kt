package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.MapColor
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlocks {
    val REGISTRY: DeferredRegister<Block> = DeferredRegister.create(BuiltInRegistries.BLOCK, Mod.MODID)

    // @formatter:off
    @JvmField val SANDBAG = registerBlock("sandbag") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.SNARE).sound(SoundType.SAND).strength(10f, 20f)) }
    @JvmField val BARBED_WIRE = registerBlock("barbed_wire") { BarbedWireBlock() }
    @JvmField val JUMP_PAD = registerBlock("jump_pad") { JumpPadBlock() }
    @JvmField val GALENA_ORE = registerBlock("galena_ore") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()) }
    @JvmField val DEEPSLATE_GALENA_ORE = registerBlock("deepslate_galena_ore") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()) }
    @JvmField val SCHEELITE_ORE = registerBlock("scheelite_ore") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()) }
    @JvmField val DEEPSLATE_SCHEELITE_ORE = registerBlock("deepslate_scheelite_ore") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()) }
    @JvmField val SILVER_ORE = registerBlock("silver_ore") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()) }
    @JvmField val DEEPSLATE_SILVER_ORE = registerBlock("deepslate_silver_ore") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()) }
    @JvmField val RAW_GALENA_BLOCK = registerBlock("raw_galena_block") { Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)) }
    @JvmField val RAW_SCHEELITE_BLOCK = registerBlock("raw_scheelite_block") { Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)) }
    @JvmField val RAW_SILVER_BLOCK = registerBlock("raw_silver_block") { Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)) }

    @JvmField val DRAGON_TEETH = registerBlock("dragon_teeth") { DragonTeethBlock() }
    @JvmField val REFORGING_TABLE = registerBlock("reforging_table") { ReforgingTableBlock() }
    @JvmField val LEAD_BLOCK = registerBlock("lead_block") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()) }
    @JvmField val STEEL_BLOCK = registerBlock("steel_block") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()) }
    @JvmField val TUNGSTEN_BLOCK = registerBlock("tungsten_block") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()) }
    @JvmField val SILVER_BLOCK = registerBlock("silver_block") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()) }
    @JvmField val CEMENTED_CARBIDE_BLOCK = registerBlock("cemented_carbide_block") { Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()) }
    @JvmField val CONTAINER = registerBlock("container") { ContainerBlock() }
    @JvmField val BIOGAS_GENERATOR = registerBlock("biogas_generator") { BiogasGeneratorBlock() }
    @JvmField val CHARGING_STATION = registerBlock("charging_station") { ChargingStationBlock() }
    @JvmField val CREATIVE_CHARGING_STATION = registerBlock("creative_charging_station") { CreativeChargingStationBlock() }
    @JvmField val FUMO_25 = registerBlock("fumo_25") { FuMO25Block() }
    @JvmField val SMALL_CONTAINER = registerBlock("small_container") { SmallContainerBlock() }
    @JvmField val VEHICLE_DEPLOYER = registerBlock("vehicle_deployer") { VehicleDeployerBlock() }
    @JvmField val AIRCRAFT_CATAPULT = registerBlock("aircraft_catapult") { AircraftCatapultBlock() }
    @JvmField val SUPERB_ITEM_INTERFACE = registerBlock("superb_item_interface") { SuperbItemInterfaceBlock() }
    @JvmField val CREATIVE_SUPERB_ITEM_INTERFACE = registerBlock("creative_superb_item_interface") { CreativeSuperbItemInterfaceBlock() }
    @JvmField val LUCKY_CONTAINER = registerBlock("lucky_container") { LuckyContainerBlock() }

    @JvmField val VEHICLE_ASSEMBLING_TABLE = registerBlock("vehicle_assembling_table") { VehicleAssemblingTableBlock() }
    @JvmField val BLUEPRINT_RESEARCH_TABLE = registerBlock("blueprint_research_table") { BlueprintResearchTableBlock() }
    // @formatter:on

    fun registerBlock(name: String, block: () -> Block): DeferredHolder<Block, out Block> {
        return REGISTRY.register(name, block)
    }
}