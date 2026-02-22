package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity;
import com.atsuishio.superbwarfare.block.entity.CreativeChargingStationBlockEntity;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyStorage;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.item.CreativeChargingStationBlockItem;
import com.atsuishio.superbwarfare.item.EnergyStorageItem;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 充电站
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CHARGING_STATION.value(), ChargingStationBlockEntity::getEnergyStorage);

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

        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof EnergyStorageItem) {
                event.registerItem(
                        Capabilities.EnergyStorage.ITEM,
                        (stack, ctx) -> new ItemEnergyStorage(stack, s -> ((EnergyStorageItem) item).getMaxEnergy(s), s -> ((EnergyStorageItem) item).getMaxReceiveEnergy(s), s -> ((EnergyStorageItem) item).getMaxExtractEnergy(s)),
                        item
                );
            }
        }

        // 载具
        for (var entity : BuiltInRegistries.ENTITY_TYPE) {
            // 能量
            event.registerEntity(Capabilities.EnergyStorage.ENTITY,
                    entity,
                    (obj, ctx) -> (obj instanceof VehicleEntity vehicle && vehicle.hasEnergyStorage()) ? vehicle.getEnergyStorage() : null
            );

            // 物品
            event.registerEntity(Capabilities.ItemHandler.ENTITY,
                    entity,
                    (obj, ctx) -> (obj instanceof VehicleEntity vehicle && vehicle.hasContainer()) ? vehicle.getInventory() : null
            );
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
