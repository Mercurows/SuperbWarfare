package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.datagen.base.SbwWreckageLootProvider
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.data.PackOutput
import net.minecraft.world.entity.EntityType
import net.neoforged.neoforge.common.data.ExistingFileHelper

typealias LootBuilder = WreckageLootData.Builder
typealias PoolBuilder = WreckageLootData.Pool.Builder
typealias Type = WreckageLootData.Pool.Type
typealias Entry = WreckageLootData.Entry

class ModWreckageLootProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    SbwWreckageLootProvider(output, existingFileHelper) {

    override fun generate() {
        createDefaultLoot(ModEntities.A_10A.get())
        createDefaultLoot(ModEntities.AH_6.get())
        createDefaultLoot(ModEntities.ANNIHILATOR.get())
        createDefaultLoot(ModEntities.BL_132.get())

        this.add(
            ModEntities.BMP_2.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 8, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get()),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get()),
                            Entry(ModItems.TRACK.get(), 2),
                            Entry(ModItems.LARGE_MOTOR.get()),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 8, 0.2),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.2),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                            Entry(ModItems.TRACK.get(), 2, 0.2),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get()),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.2),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 6, 1.0),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get()),
                            Entry(ModItems.TRACK.get(), 2),
                            Entry(ModItems.LARGE_MOTOR.get()),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 6, 0.2),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                            Entry(ModItems.TRACK.get(), 2, 0.2),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.2),
                        ).build(),
                )
        )

        createDefaultLoot(ModEntities.BRADLEY.get())
        createDefaultLoot(ModEntities.HPJ_11.get())
        createDefaultLoot(ModEntities.JU_87.get())
        createDefaultLoot(ModEntities.KV_16.get())
        createDefaultLoot(ModEntities.LAV_150.get())
        createDefaultLoot(ModEntities.LAV_AD.get())
        createDefaultLoot(ModEntities.M_1A_2.get())
        createDefaultLoot(ModEntities.MI_28.get())
        createDefaultLoot(ModEntities.MK_42.get())
        createDefaultLoot(ModEntities.MLE_1934.get())
        createDefaultLoot(ModEntities.PLZ_05.get())
        createDefaultLoot(ModEntities.PRISM_TANK.get())
        createDefaultLoot(ModEntities.SODAYO_PICK_UP.get())
        createDefaultLoot(ModEntities.SODAYO_PICK_UP_HMG.get())
        createDefaultLoot(ModEntities.SODAYO_PICK_UP_ROCKET.get())
        createDefaultLoot(ModEntities.SODAYO_PICK_UP_TOW.get())
        createDefaultLoot(ModEntities.SPEEDBOAT.get())
        createDefaultLoot(ModEntities.T_90A.get())
        createDefaultLoot(ModEntities.TRUCK.get())
        createDefaultLoot(ModEntities.WAVEFORCE_TOWER.get())
        createDefaultLoot(ModEntities.YX_100.get())
    }

    override fun getName(): String = "Superb Warfare Wreckage Loot"

    private fun createDefaultLoot(type: EntityType<out VehicleEntity>) {
        this.add(
            type,
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                        ),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.2),
                        ),
                )
        )
    }
}