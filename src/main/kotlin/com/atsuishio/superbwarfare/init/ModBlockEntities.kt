package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.block.entity.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModBlockEntities {
    @JvmField
    val REGISTRY: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Mod.MODID)

    inline fun <reified T : BlockEntity> registerBlockEntity(
        id: String,
        noinline blockEntity: () -> BlockEntityType<T>
    ): RegistryObject<BlockEntityType<T>> =
        REGISTRY.register(id, blockEntity)

    @JvmField
    val CONTAINER = registerBlockEntity("container") {
        BlockEntityType.Builder.of(
            { pos, state -> ContainerBlockEntity(pos, state) },
            ModBlocks.CONTAINER.get()
        ).build(null)
    }

    @JvmField
    val CHARGING_STATION = registerBlockEntity("charging_station") {
        BlockEntityType.Builder.of(
            { pos, state -> ChargingStationBlockEntity(pos, state) },
            ModBlocks.CHARGING_STATION.get()
        ).build(null)
    }

    @JvmField
    val CREATIVE_CHARGING_STATION = registerBlockEntity("creative_charging_station") {
        BlockEntityType.Builder.of(
            { pos, state -> CreativeChargingStationBlockEntity(pos, state) },
            ModBlocks.CREATIVE_CHARGING_STATION.get()
        ).build(null)
    }

    @JvmField
    val FUMO_25 = registerBlockEntity("fumo_25") {
        BlockEntityType.Builder.of(
            { pos, state -> FuMO25BlockEntity(pos, state) },
            ModBlocks.FUMO_25.get()
        ).build(null)
    }

    @JvmField
    val SMALL_CONTAINER = registerBlockEntity("small_container") {
        BlockEntityType.Builder.of(
            { pos, state -> SmallContainerBlockEntity(pos, state) },
            ModBlocks.SMALL_CONTAINER.get()
        ).build(null)
    }

    @JvmField
    val VEHICLE_DEPLOYER = registerBlockEntity("vehicle_deployer") {
        BlockEntityType.Builder.of(
            { pos, state -> VehicleDeployerBlockEntity(pos, state) },
            ModBlocks.VEHICLE_DEPLOYER.get()
        ).build(null)
    }

    @JvmField
    val SUPERB_ITEM_INTERFACE = registerBlockEntity("superb_item_interface") {
        BlockEntityType.Builder.of(
            { pos, state -> SuperbItemInterfaceBlockEntity(pos, state) },
            ModBlocks.SUPERB_ITEM_INTERFACE.get()
        ).build(null)
    }

    @JvmField
    val CREATIVE_SUPERB_ITEM_INTERFACE = registerBlockEntity("creative_superb_item_interface") {
        BlockEntityType.Builder.of(
            { pos, state -> CreativeSuperbItemInterfaceBlockEntity(pos, state) },
            ModBlocks.CREATIVE_SUPERB_ITEM_INTERFACE.get()
        ).build(null)
    }

    @JvmField
    val LUCKY_CONTAINER = registerBlockEntity("lucky_container") {
        BlockEntityType.Builder.of(
            { pos, state -> LuckyContainerBlockEntity(pos, state) },
            ModBlocks.LUCKY_CONTAINER.get()
        ).build(null)
    }

    @JvmField
    val VEHICLE_ASSEMBLING_TABLE = registerBlockEntity("vehicle_assembling_table") {
        BlockEntityType.Builder.of(
            { pos, state -> VehicleAssemblingTableBlockEntity(pos, state) },
            ModBlocks.VEHICLE_ASSEMBLING_TABLE.get()
        ).build(null)
    }

    @JvmField
    val BLUEPRINT_RESEARCH_TABLE = registerBlockEntity("blueprint_research_table") {
        BlockEntityType.Builder.of(
            { pos, state -> BlueprintResearchTableBlockEntity(pos, state) },
            ModBlocks.BLUEPRINT_RESEARCH_TABLE.get()
        ).build(null)
    }

    @JvmField
    val BIOGAS_GENERATOR = registerBlockEntity("biogas_generator") {
        BlockEntityType.Builder.of(
            { pos, state -> BiogasGeneratorBlockEntity(pos, state) },
            ModBlocks.BIOGAS_GENERATOR.get()
        ).build(null)
    }
}