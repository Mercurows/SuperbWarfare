package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("unused")
public class ModBlocks {

    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK, Mod.MODID);

    public static final DeferredHolder<Block, Block> SANDBAG = REGISTRY.register("sandbag",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.SNARE).sound(SoundType.SAND).strength(10f, 20f)));
    public static final DeferredHolder<Block, Block> BARBED_WIRE = REGISTRY.register("barbed_wire", BarbedWireBlock::new);
    public static final DeferredHolder<Block, Block> JUMP_PAD = REGISTRY.register("jump_pad", JumpPadBlock::new);
    public static final DeferredHolder<Block, Block> GALENA_ORE = REGISTRY.register("galena_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> DEEPSLATE_GALENA_ORE = REGISTRY.register("deepslate_galena_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> SCHEELITE_ORE = REGISTRY.register("scheelite_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> DEEPSLATE_SCHEELITE_ORE = REGISTRY.register("deepslate_scheelite_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> SILVER_ORE = REGISTRY.register("silver_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> DEEPSLATE_SILVER_ORE = REGISTRY.register("deepslate_silver_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> DRAGON_TEETH = REGISTRY.register("dragon_teeth", DragonTeethBlock::new);
    public static final DeferredHolder<Block, Block> REFORGING_TABLE = REGISTRY.register("reforging_table", ReforgingTableBlock::new);
    public static final DeferredHolder<Block, Block> LEAD_BLOCK = REGISTRY.register("lead_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> STEEL_BLOCK = REGISTRY.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> TUNGSTEN_BLOCK = REGISTRY.register("tungsten_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> SILVER_BLOCK = REGISTRY.register("silver_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> CEMENTED_CARBIDE_BLOCK = REGISTRY.register("cemented_carbide_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, ContainerBlock> CONTAINER = REGISTRY.register("container", () -> new ContainerBlock());
    public static final DeferredHolder<Block, ChargingStationBlock> CHARGING_STATION = REGISTRY.register("charging_station", ChargingStationBlock::new);
    public static final DeferredHolder<Block, CreativeChargingStationBlock> CREATIVE_CHARGING_STATION = REGISTRY.register("creative_charging_station", () -> new CreativeChargingStationBlock());
    public static final DeferredHolder<Block, FuMO25Block> FUMO_25 = REGISTRY.register("fumo_25", FuMO25Block::new);
    public static final DeferredHolder<Block, SmallContainerBlock> SMALL_CONTAINER = REGISTRY.register("small_container", () -> new SmallContainerBlock());
    public static final DeferredHolder<Block, VehicleDeployerBlock> VEHICLE_DEPLOYER = REGISTRY.register("vehicle_deployer", VehicleDeployerBlock::new);
    public static final DeferredHolder<Block, AircraftCatapultBlock> AIRCRAFT_CATAPULT = REGISTRY.register("aircraft_catapult", AircraftCatapultBlock::new);
    public static final DeferredHolder<Block, SuperbItemInterfaceBlock> SUPERB_ITEM_INTERFACE = REGISTRY.register("superb_item_interface", () -> new SuperbItemInterfaceBlock());
}
