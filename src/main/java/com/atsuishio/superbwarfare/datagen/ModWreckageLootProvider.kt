package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.datagen.base.SbwWreckageLootProvider
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.data.PackOutput
import net.minecraftforge.common.data.ExistingFileHelper

typealias LootBuilder = WreckageLootData.Builder
typealias PoolBuilder = WreckageLootData.Pool.Builder
typealias Type = WreckageLootData.Pool.Type
typealias Entry = WreckageLootData.Entry

class ModWreckageLootProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    SbwWreckageLootProvider(output, existingFileHelper) {

    override fun generate() {
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
    }

    override fun getName(): String = "Superb Warfare Wreckage Loot"
}