package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity;
import com.atsuishio.superbwarfare.block.entity.CreativeChargingStationBlockEntity;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyStorage;
import com.atsuishio.superbwarfare.capability.laser.LaserCapability;
import com.atsuishio.superbwarfare.capability.laser.LaserCapabilityProvider;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.EnergyVehicleEntity;
import com.atsuishio.superbwarfare.item.CreativeChargingStationBlockItem;
import com.atsuishio.superbwarfare.item.EnergyStorageItem;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    public static final EntityCapability<LaserCapability, Void> LASER_CAPABILITY = EntityCapability.createVoid(Mod.loc("laser_capability"), LaserCapability.class);

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 激光
        event.registerEntity(ModCapabilities.LASER_CAPABILITY, EntityType.PLAYER, new LaserCapabilityProvider());

        // 充电站
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CHARGING_STATION.value(), ChargingStationBlockEntity::getEnergyStorage);
        // TODO 这注册对吗？
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHARGING_STATION.value(), (object, context) -> {
            if (context == null || object.isRemoved()) return null;

            var itemHandlers = new IItemHandler[]{
                    new SidedInvWrapper(object, Direction.UP),
                    new SidedInvWrapper(object, Direction.DOWN),
                    new SidedInvWrapper(object, Direction.NORTH),
            };

            return switch (context) {
                case UP -> itemHandlers[0];
                case DOWN -> itemHandlers[1];
                default -> itemHandlers[2];
            };
        });

        // 创造模式充电站
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CREATIVE_CHARGING_STATION.value(), CreativeChargingStationBlockEntity::getEnergyStorage);
        event.registerItem(Capabilities.EnergyStorage.ITEM, (obj, ctx) -> ((CreativeChargingStationBlockItem) obj.getItem()).getEnergyStorage(), ModItems.CREATIVE_CHARGING_STATION.value());

        // FuMO25
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.FUMO_25.value(), FuMO25BlockEntity::getEnergyStorage);

        // 所有能存能量的物品
        var list = new ArrayList<DeferredHolder<Item, ? extends Item>>();
        list.addAll(ModItems.ITEMS.getEntries());
        list.addAll(ModItems.GUNS.getEntries());

        for (var item : list) {
            if (item.get() instanceof EnergyStorageItem) {
                event.registerItem(
                        Capabilities.EnergyStorage.ITEM,
                        (stack, ctx) -> new ItemEnergyStorage(stack, ((EnergyStorageItem) stack.getItem()).getMaxEnergy()),
                        item.get()
                );
            }
        }

        // 载具
        for (var entity : ModEntities.REGISTRY.getEntries()) {
            // 能量
            if (entity.get().getBaseClass().isAssignableFrom(EnergyVehicleEntity.class)) {
                event.registerEntity(Capabilities.EnergyStorage.ENTITY,
                        entity.get(),
                        (obj, ctx) -> (obj instanceof EnergyVehicleEntity vehicle) ? vehicle.getEnergyStorage() : null
                );
            }

            // 物品
            if (entity.get().getBaseClass().isAssignableFrom(ContainerMobileVehicleEntity.class)) {
                event.registerEntity(Capabilities.ItemHandler.ENTITY,
                        entity.get(),
                        (obj, ctx) -> (obj instanceof ContainerMobileVehicleEntity vehicle) ? new InvWrapper(vehicle) : null
                );
            }
        }

        // DPS发电机
        event.registerEntity(Capabilities.EnergyStorage.ENTITY,
                ModEntities.DPS_GENERATOR.get(),
                (obj, ctx) -> obj.getEnergyStorage()
        );

        // 卓越物品接口
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SUPERB_ITEM_INTERFACE.get(),
                (object, context) -> new InvWrapper(object)
        );

    }
}
