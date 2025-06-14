package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Mod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ContainerBlockEntity>> CONTAINER = REGISTRY.register("container",
            () -> BlockEntityType.Builder.of(ContainerBlockEntity::new, ModBlocks.CONTAINER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmallContainerBlockEntity>> SMALL_CONTAINER = REGISTRY.register("small_container",
            () -> BlockEntityType.Builder.of(SmallContainerBlockEntity::new, ModBlocks.SMALL_CONTAINER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION = REGISTRY.register("charging_station",
            () -> BlockEntityType.Builder.of(ChargingStationBlockEntity::new, ModBlocks.CHARGING_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeChargingStationBlockEntity>> CREATIVE_CHARGING_STATION = REGISTRY.register("creative_charging_station",
            () -> BlockEntityType.Builder.of(CreativeChargingStationBlockEntity::new, ModBlocks.CREATIVE_CHARGING_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuMO25BlockEntity>> FUMO_25 = REGISTRY.register("fumo_25",
            () -> BlockEntityType.Builder.of(FuMO25BlockEntity::new, ModBlocks.FUMO_25.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VehicleDeployerBlockEntity>> VEHICLE_DEPLOYER = REGISTRY.register("vehicle_deployer",
            () -> BlockEntityType.Builder.of(VehicleDeployerBlockEntity::new, ModBlocks.VEHICLE_DEPLOYER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SuperbItemInterfaceBlockEntity>> SUPERB_ITEM_INTERFACE = REGISTRY.register("superb_item_interface",
            () -> BlockEntityType.Builder.of(SuperbItemInterfaceBlockEntity::new, ModBlocks.SUPERB_ITEM_INTERFACE.get()).build(null));
}
