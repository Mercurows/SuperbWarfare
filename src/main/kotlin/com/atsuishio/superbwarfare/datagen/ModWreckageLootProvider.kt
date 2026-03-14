package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.data.loot.WreckageLootData
import com.atsuishio.superbwarfare.datagen.base.SbwWreckageLootProvider
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.data.PackOutput
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Items
import net.neoforged.neoforge.common.data.ExistingFileHelper

typealias LootBuilder = WreckageLootData.Builder
typealias PoolBuilder = WreckageLootData.Pool.Builder
typealias Type = WreckageLootData.Pool.Type
typealias Entry = WreckageLootData.Entry

class ModWreckageLootProvider(output: PackOutput, existingFileHelper: ExistingFileHelper) :
    SbwWreckageLootProvider(output, existingFileHelper) {

    override fun generate() {
        this.add(
            ModEntities.A_10A.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.WHEEL.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5),
                        ).build()
                )
        )

        this.add(
            ModEntities.AH_6.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.LIGHT_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5),
                        ).build()
                )
        )

        this.add(
            ModEntities.ANNIHILATOR.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 12, 1.0),
                            Entry(ModItems.LASER_UNIT.get(), 16, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.5),
                            Entry(Items.NETHERITE_BLOCK, 3, 1.0),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 12, 0.5),
                            Entry(Items.NETHERITE_BLOCK, 1, 0.25),
                        ).build()
                )
        )

        this.add(
            ModEntities.BL_132.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 5, 1.0),
                            Entry(ModItems.CANNON_CORE.get(), 2, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.BMP_2.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.BRADLEY.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.HPJ_11.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.CANNON_CORE.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.JU_87.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5),
                        ).build()
                )
        )

        this.add(
            ModEntities.KV_16.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(Items.BUCKET, 2, 1.0),
                            Entry(ModItems.STEEL_INGOT.get(), 4, 1.0),
                            Entry(ModItems.LIGHT_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.SMALL_BATTERY_PACK.get(), 1, 0.5),
                            Entry(ModItems.PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(Items.BUCKET, 2, 1.0),
                            Entry(ModItems.STEEL_INGOT.get(), 4, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.LAV_25.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.WHEEL.get(), 4, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.WHEEL.get(), 4, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.LAV_150.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.LIGHT_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.WHEEL.get(), 2, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.LIGHT_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.WHEEL.get(), 2, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.LAV_AD.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.WHEEL.get(), 4, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.WHEEL.get(), 4, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.M_1A_2.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 6, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.T_90A.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 6, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.MI_28.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5),
                        ).build()
                )
        )

        this.add(
            ModEntities.MK_42.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.CANNON_CORE.get(), 1, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.MLE_1934.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.CANNON_CORE.get(), 2, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.PLZ_05.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 5, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.CANNON_CORE.get(), 2, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 5, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.CANNON_CORE.get(), 2, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 3, 0.5)
                        ).build(),
                )
        )

        this.add(
            ModEntities.PRISM_TANK.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LASER_UNIT.get(), 8, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.SODAYO_PICK_UP.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.WHEEL.get(), 2, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.SODAYO_PICK_UP_HMG.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.LIGHT_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.WHEEL.get(), 2, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.SODAYO_PICK_UP_ROCKET.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.MORTAR_BARREL.get(), 6, 1.0),
                            Entry(ModItems.WHEEL.get(), 2, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.SODAYO_PICK_UP_TOW.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.MORTAR_BARREL.get(), 1, 0.5),
                            Entry(ModItems.ARTILLERY_INDICATOR.get(), 1, 0.5),
                            Entry(ModItems.WHEEL.get(), 2, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.SPEEDBOAT.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 1.0),
                            Entry(ModItems.LIGHT_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.LARGE_PROPELLER.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.SMALL_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 1, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.TRUCK.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.WHEEL.get(), 3, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.WAVEFORCE_TOWER.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 5, 1.0),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 1, 0.5),
                            Entry(Items.REDSTONE_BLOCK, 4, 0.5),
                            Entry(ModItems.LASER_UNIT.get(), 4, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.2)
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 5, 0.5)
                        ).build()
                )
        )

        this.add(
            ModEntities.YX_100.get(),
            LootBuilder()
                .addPool(
                    PoolBuilder(type = Type.COMPLETE)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 1.0),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 12, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.COMPLETE)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 4, 0.5),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 12, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.HEAVY_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.MEDIUM_ARMAMENT_MODULE.get(), 1, 0.5),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 3, 1.0)
                        ).build(),
                    PoolBuilder(type = Type.TURRET_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 3, 0.5)
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .source(ModDamageTypes.REPAIR_TOOL)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 1.0),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 9, 1.0),
                            Entry(ModItems.TRACK.get(), 1, 0.5),
                            Entry(ModItems.LARGE_MOTOR.get(), 1, 0.5),
                            Entry(ModItems.LARGE_BATTERY_PACK.get(), 1, 0.2),
                        ).build(),
                    PoolBuilder(type = Type.VEHICLE_ONLY)
                        .addEntry(
                            Entry(ModItems.STEEL_BLOCK.get(), 2, 0.5),
                            Entry(ModItems.CEMENTED_CARBIDE_BLOCK.get(), 9, 0.5)
                        ).build(),
                )
        )
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