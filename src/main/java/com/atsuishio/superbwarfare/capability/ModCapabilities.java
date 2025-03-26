package com.atsuishio.superbwarfare.capability;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity;
import com.atsuishio.superbwarfare.block.entity.CreativeChargingStationBlockEntity;
import com.atsuishio.superbwarfare.capability.energy.BlockEnergyStorageProvider;
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider;
import com.atsuishio.superbwarfare.capability.laser.LaserCapability;
import com.atsuishio.superbwarfare.capability.laser.LaserCapabilityProvider;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.capability.player.PlayerVariablesProvider;
import com.atsuishio.superbwarfare.init.ModBlockEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.CreativeChargingStationBlockItem;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = ModUtils.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModCapabilities {

    public static final EntityCapability<LaserCapability, Void> LASER_CAPABILITY = EntityCapability.createVoid(ModUtils.loc("laser_capability"), LaserCapability.class);
    public static final EntityCapability<PlayerVariable, Void> PLAYER_VARIABLE = EntityCapability.createVoid(ModUtils.loc("player_variable"), PlayerVariable.class);

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(ModCapabilities.LASER_CAPABILITY, EntityType.PLAYER, new LaserCapabilityProvider());
        event.registerEntity(ModCapabilities.PLAYER_VARIABLE, EntityType.PLAYER, new PlayerVariablesProvider());

        // 充电站
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CHARGING_STATION.value(), new BlockEnergyStorageProvider<>(ChargingStationBlockEntity.MAX_ENERGY));
        // TODO HANDLER
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHARGING_STATION.value(), new ChargingStationBlockEntity.ItemHandlerProvider());

        // 创造模式充电站
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CREATIVE_CHARGING_STATION.value(), new CreativeChargingStationBlockEntity.EnergyStorageProvider());
        event.registerItem(Capabilities.EnergyStorage.ITEM, new CreativeChargingStationBlockItem.EnergyStorageProvider(), ModItems.CREATIVE_CHARGING_STATION.value());

        // 电池
        for (var item : ModItems.ITEMS.getEntries()) {
            if (item.get() instanceof BatteryItem battery) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, new ItemEnergyProvider(battery.maxEnergy), battery);
            }
        }

    }
}
