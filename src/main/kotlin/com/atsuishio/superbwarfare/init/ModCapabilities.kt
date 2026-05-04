package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyStorage
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.item.EnergyStorageItem
import com.atsuishio.superbwarfare.item.blockitem.CreativeChargingStationBlockItem
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.items.wrapper.InvWrapper
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper

@EventBusSubscriber(modid = Mod.MODID)
object ModCapabilities {
    @SubscribeEvent
    fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        // 充电站
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CHARGING_STATION.value()
        ) { obj, side -> obj.getEnergyStorage(side) }

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK, ModBlockEntities.CHARGING_STATION.value()
        ) { obj, context ->
            if (context == null || obj.isRemoved) return@registerBlockEntity null

            val itemHandlers = arrayOf(
                SidedInvWrapper(obj, Direction.UP),
                SidedInvWrapper(obj, Direction.DOWN),
                SidedInvWrapper(obj, Direction.NORTH),
            )

            when (context) {
                Direction.UP -> itemHandlers[0]
                Direction.DOWN -> itemHandlers[1]
                else -> itemHandlers[2]
            }
        }

        // 创造模式充电站
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK, ModBlockEntities.CREATIVE_CHARGING_STATION.value()
        ) { obj, side -> obj.getEnergyStorage(side) }
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            { obj, _ -> (obj.item as CreativeChargingStationBlockItem).energyStorage },
            ModItems.CREATIVE_CHARGING_STATION.value()
        )

        // FuMO25
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK, ModBlockEntities.FUMO_25.value()
        ) { obj, direction -> obj.getEnergyStorage() }

        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK, ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.value()
        ) { obj, direction ->
            if (obj.isRemoved) return@registerBlockEntity null

            val itemHandlers = arrayOf(
                SidedInvWrapper(obj, Direction.UP),
                SidedInvWrapper(obj, Direction.DOWN),
                SidedInvWrapper(obj, Direction.NORTH),
                SidedInvWrapper(obj, Direction.SOUTH),
                SidedInvWrapper(obj, Direction.EAST),
            )

            when (direction) {
                Direction.UP -> itemHandlers[0]
                Direction.DOWN -> itemHandlers[1]
                Direction.NORTH -> itemHandlers[2]
                Direction.SOUTH -> itemHandlers[3]
                else -> itemHandlers[4]
            }
        }

        for (item in BuiltInRegistries.ITEM) {
            if (item is EnergyStorageItem) {
                event.registerItem(Capabilities.EnergyStorage.ITEM, { stack, _ ->
                    ItemEnergyStorage(
                        stack,
                        { s -> item.getMaxEnergy(s) },
                        { s -> item.getMaxReceiveEnergy(s) },
                        { s -> item.getMaxExtractEnergy(s) }
                    )
                }, item)
            }
        }

        // 载具
        for (entity in BuiltInRegistries.ENTITY_TYPE) {
            // 能量
            event.registerEntity(
                Capabilities.EnergyStorage.ENTITY, entity
            ) { obj, _ -> if (obj is VehicleEntity && obj.hasEnergyStorage()) obj.getEnergyStorage() else null }

            // 物品
            event.registerEntity(
                Capabilities.ItemHandler.ENTITY, entity
            ) { obj, _ -> if (obj is VehicleEntity && obj.hasContainer()) obj.inventory else null }
        }

        // DPS发电机
        event.registerEntity(
            Capabilities.EnergyStorage.ENTITY, ModEntities.DPS_GENERATOR.get()
        ) { obj, _ -> obj.energyStorage }

        // 卓越物品接口
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK, ModBlockEntities.SUPERB_ITEM_INTERFACE.get()
        ) { obj, _ -> InvWrapper(obj) }
    }
}
