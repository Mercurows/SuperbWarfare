package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, Mod.MODID);

    public static final RegistryObject<Block> SANDBAG = REGISTRY.register("sandbag",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.SNARE).sound(SoundType.SAND).strength(10f, 20f)));
    public static final RegistryObject<Block> BARBED_WIRE = REGISTRY.register("barbed_wire", BarbedWireBlock::new);
    public static final RegistryObject<Block> JUMP_PAD = REGISTRY.register("jump_pad", JumpPadBlock::new);
    public static final RegistryObject<Block> GALENA_ORE = REGISTRY.register("galena_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DEEPSLATE_GALENA_ORE = REGISTRY.register("deepslate_galena_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SCHEELITE_ORE = REGISTRY.register("scheelite_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DEEPSLATE_SCHEELITE_ORE = REGISTRY.register("deepslate_scheelite_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SILVER_ORE = REGISTRY.register("silver_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 5f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DEEPSLATE_SILVER_ORE = REGISTRY.register("deepslate_silver_ore",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(3f, 8f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> DRAGON_TEETH = REGISTRY.register("dragon_teeth", DragonTeethBlock::new);
    public static final RegistryObject<Block> REFORGING_TABLE = REGISTRY.register("reforging_table", ReforgingTableBlock::new);
    public static final RegistryObject<Block> LEAD_BLOCK = REGISTRY.register("lead_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> STEEL_BLOCK = REGISTRY.register("steel_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> TUNGSTEN_BLOCK = REGISTRY.register("tungsten_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SILVER_BLOCK = REGISTRY.register("silver_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CEMENTED_CARBIDE_BLOCK = REGISTRY.register("cemented_carbide_block",
            () -> new Block(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5f, 6f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> CONTAINER = REGISTRY.register("container", ContainerBlock::new);
    public static final RegistryObject<Block> CHARGING_STATION = REGISTRY.register("charging_station", ChargingStationBlock::new);
    public static final RegistryObject<Block> CREATIVE_CHARGING_STATION = REGISTRY.register("creative_charging_station", CreativeChargingStationBlock::new);
    public static final RegistryObject<Block> FUMO_25 = REGISTRY.register("fumo_25", FuMO25Block::new);
    public static final RegistryObject<Block> SMALL_CONTAINER = REGISTRY.register("small_container", SmallContainerBlock::new);
    public static final RegistryObject<Block> VEHICLE_DEPLOYER = REGISTRY.register("vehicle_deployer", VehicleDeployerBlock::new);
    public static final RegistryObject<Block> AIRCRAFT_CATAPULT = REGISTRY.register("aircraft_catapult", AircraftCatapultBlock::new);
    public static final RegistryObject<Block> SUPERB_ITEM_INTERFACE = REGISTRY.register("superb_item_interface", SuperbItemInterfaceBlock::new);
    public static final RegistryObject<Block> CREATIVE_SUPERB_ITEM_INTERFACE = REGISTRY.register("creative_superb_item_interface", CreativeSuperbItemInterfaceBlock::new);
    public static final RegistryObject<Block> LUCKY_CONTAINER = REGISTRY.register("lucky_container", LuckyContainerBlock::new);
}
