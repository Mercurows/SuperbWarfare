package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.SpawnConfig
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.entity.living.SenpaiEntity
import com.atsuishio.superbwarfare.entity.living.SteelCoilEntity
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.projectile.*
import com.atsuishio.superbwarfare.entity.vehicle.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.Difficulty
import net.minecraft.world.entity.*
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

@EventBusSubscriber
object ModEntities {
    val REGISTRY: DeferredRegister<EntityType<*>> = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Mod.MODID)

    // Living Entities
    @JvmField
    val TARGET = register(
        "target",
        EntityType.Builder.of(::TargetEntity, MobCategory.CREATURE)
            .setTrackingRange(64).setUpdateInterval(3).fireImmune().eyeHeight(1.57f).sized(0.875f, 2f)
    )

    @JvmField
    val DPS_GENERATOR = register(
        "dps_generator",
        EntityType.Builder.of(::DPSGeneratorEntity, MobCategory.CREATURE)
            .setTrackingRange(64).setUpdateInterval(3).fireImmune().eyeHeight(1.57f).sized(0.875f, 2f)
    )

    @JvmField
    val SENPAI = register(
        "senpai",
        EntityType.Builder.of(::SenpaiEntity, MobCategory.MONSTER)
            .setTrackingRange(64).setUpdateInterval(3).sized(0.65f, 2f).eyeHeight(1.75f)
    )

    @JvmField
    val STEEL_COIL = register(
        "steel_coil", EntityType.Builder.of(::SteelCoilEntity, MobCategory.MONSTER)
            .setTrackingRange(64).setUpdateInterval(3).sized(2f, 2f).fireImmune()
    )

    // Misc Entities
    @JvmField
    val FLARE_DECOY = register(
        "flare_decoy",
        misc(::FlareDecoyEntity).setTrackingRange(64).setUpdateInterval(1).noSave().sized(1f, 1f)
    )

    @JvmField
    val PRISMATIC_BOLT = register(
        "prismatic_bolt",
        misc(::PrismaticBoltEntity).setTrackingRange(64).setUpdateInterval(1).noSave().noSummon().fireImmune()
            .sized(0.05f, 0.05f)
    )

    @JvmField
    val SMOKE_DECOY = register(
        "smoke_decoy",
        misc(::SmokeDecoyEntity).setTrackingRange(64).setUpdateInterval(1).noSave().sized(5.5f, 5.5f)
    )

    @JvmField
    val CLAYMORE = register(
        "claymore",
        misc(::ClaymoreEntity).setTrackingRange(64).setUpdateInterval(1).sized(0.25f, 0.25f)
    )

    @JvmField
    val BLU_43 = register(
        "blu_43",
        misc(::Blu43Entity).setTrackingRange(32).setUpdateInterval(1).sized(0.12f, 0.05f)
    )

    @JvmField
    val TM_62 = register("tm_62", misc(::Tm62Entity).setTrackingRange(32).setUpdateInterval(1).sized(0.5f, 0.15f))

    @JvmField
    val PTKM_1R = register(
        "ptkm_1r",
        misc(::Ptkm1rEntity).setTrackingRange(64).setUpdateInterval(1).sized(0.2f, 0.7f)
    )

    @JvmField
    val C4 = register("c4", misc(::C4Entity).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f))

    @JvmField
    val MEDICAL_KIT = register(
        "medical_kit",
        misc(::MedicalKitEntity).setTrackingRange(64).setUpdateInterval(1).sized(0.4f, 0.2f)
    )

    @JvmField
    val EDD = register("edd", misc(::EDDEntity).setTrackingRange(10).eyeHeight(0f).setUpdateInterval(Int.MAX_VALUE).sized(0.5f, 0.5f))

    // Projectiles
    @JvmField
    val TASER_BULLET = register("taser_bullet", fastProjectile(::TaserBulletEntity, true).sized(0.25f, 0.25f))

    @JvmField
    val WHITE_PHOSPHORUS_PROJECTILE = register(
        "white_phosphorus_projectile",
        fastProjectile(::WhitePhosphorusProjectileEntity, true).sized(0.1f, 0.1f)
    )

    // Fast Projectiles
    @JvmField
    val SUPER_STAR_PROJECTILE =
        register("super_star_projectile", fastProjectile(::SuperStarProjectileEntity).sized(0.75f, 0.75f))

    @JvmField
    val SMALL_CANNON_SHELL =
        register("small_cannon_shell", fastProjectile(::SmallCannonShellEntity).sized(0.25f, 0.25f))

    @JvmField
    val RPG_ROCKET_TBG = register("rpg_rocket_tbg", fastProjectile(::RpgRocketTBGEntity).sized(0.5f, 0.5f))

    @JvmField
    val RPG_ROCKET_STANDARD =
        register("rpg_rocket_standard", fastProjectile(::RpgRocketStandardEntity).sized(0.5f, 0.5f))

    @JvmField
    val MORTAR_SHELL = register("mortar_shell", fastProjectile(::MortarShellEntity).sized(0.5f, 0.5f))

    @JvmField
    val PROJECTILE = register("projectile", fastProjectile(::ProjectileEntity).sized(0.25f, 0.25f))

    @JvmField
    val CANNON_SHELL = register("cannon_shell", fastProjectile(::CannonShellEntity).sized(0.75f, 0.75f))

    @JvmField
    val GUN_GRENADE = register("gun_grenade", fastProjectile(::GunGrenadeEntity).sized(0.5f, 0.5f))

    @JvmField
    val GRAPESHOT = register("grapeshot", fastProjectile(::GrapeshotEntity).sized(0.5f, 0.5f))

    @JvmField
    val MELON_BOMB = register("melon_bomb", fastProjectile(::MelonBombEntity).sized(1f, 1f))

    @JvmField
    val PTKM_PROJECTILE = register("ptkm_projectile", fastProjectile(::PtkmProjectileEntity).sized(0.5f, 0.5f))

    @JvmField
    val HAND_GRENADE = register("hand_grenade", fastProjectile(::HandGrenadeEntity, true).sized(0.3f, 0.3f))

    @JvmField
    val RGO_GRENADE = register("rgo_grenade", fastProjectile(::RgoGrenadeEntity, true).sized(0.3f, 0.3f))

    @JvmField
    val M18_SMOKE_GRENADE =
        register("m18_smoke_grenade", fastProjectile(::M18SmokeGrenadeEntity, true).sized(0.3f, 0.3f))

    @JvmField
    val JAVELIN_MISSILE = register("javelin_missile", fastProjectile(::JavelinMissileEntity).sized(0.5f, 0.5f))

    @JvmField
    val IGLA_MISSILE = register("igla_9k38_missile", fastProjectile(::IglaMissileEntity).sized(0.5f, 0.5f))

    @JvmField
    val RU_9M336_MISSILE = register("ru_9m336_missile", fastProjectile(::Ru9m336MissileEntity).sized(0.5f, 0.5f))

    @JvmField
    val AGM_65 = register("agm_65", fastProjectile(::Agm65Entity).sized(0.75f, 0.75f))

    @JvmField
    val KH_39 = register("kh_39", fastProjectile(::Kh39Entity).sized(0.75f, 0.75f))

    @JvmField
    val SMALL_ROCKET = register("small_rocket", fastProjectile(::SmallRocketEntity).sized(0.5f, 0.5f))

    @JvmField
    val MEDIUM_ROCKET = register("medium_rocket", fastProjectile(::MediumRocketEntity).sized(0.5f, 0.5f))

    @JvmField
    val WIRE_GUIDE_MISSILE =
        register("wire_guide_missile", fastProjectile(::WireGuideMissileEntity).fireImmune().sized(0.5f, 0.5f))

    @JvmField
    val SWARM_DRONE = register("swarm_drone", fastProjectile(::SwarmDroneEntity).fireImmune().sized(0.5f, 0.5f))

    @JvmField
    val MK_82 = register("mk_82", fastProjectile(::Mk82Entity).sized(0.8f, 0.8f))

    @JvmField
    val SC_250 = register("sc_250", fastProjectile(::Sc250Entity).sized(0.7f, 0.7f))

    @JvmField
    val SC_50 = register("sc_50", fastProjectile(::Sc50Entity).sized(0.4f, 0.4f))

    // Vehicles
    // Turrets
    @JvmField
    val TYPE_63 = register("type_63", vehicle(::Type63Entity).sized(1f, 1.5f))

    @JvmField
    val MK_42 = register("mk_42", vehicle(::Mk42Entity).sized(3.4f, 3.5f))

    @JvmField
    val HPJ_11 = register("hpj_11", vehicle(::Hpj11Entity).sized(2.8f, 2.4f))

    @JvmField
    val MLE_1934 = register("mle_1934", vehicle(::Mle1934Entity).sized(4.5f, 2.8f))

    @JvmField
    val BL_132 = register("bl_132", vehicle(::Bl132Entity).sized(7f, 4.4375f))

    @JvmField
    val ANNIHILATOR = register("annihilator", vehicle(::AnnihilatorEntity).sized(13f, 4.2f))

    @JvmField
    val LASER_TOWER = register("laser_tower", vehicle(::LaserTowerEntity).sized(0.9f, 1.65f))

    @JvmField
    val WAVEFORCE_TOWER = register("waveforce_tower", vehicle(::WaveforceTowerEntity).sized(1.75f, 3.3f))

    @JvmField
    val TOW = register("tow", vehicle(::TowEntity).sized(0.5f, 1.35f))

    // Boats
    @JvmField
    val SPEEDBOAT = register("speedboat", vehicle(::SpeedboatEntity).sized(3.0f, 2.0f))

    @JvmField
    val TINY_SPEEDBOAT = register("tiny_speedboat", vehicle(::TinySpeedboatEntity).sized(1.4f, 0.6f))

    // Land Vehicles
    @JvmField
    val WHEEL_CHAIR = register("wheel_chair", vehicle(::WheelChairEntity).sized(1.0f, 1.0f))

    @JvmField
    val LAV_150 = register("lav_150", vehicle(::Lav150Entity).sized(2.8f, 2.45f))

    @JvmField
    val LAV_AD = register("lav_ad", vehicle(::LavAdEntity).sized(2.8f, 2.35f))

    @JvmField
    val LAV_25 = register("lav_25", vehicle(::Lav25Entity).sized(2.8f, 2.35f))

    @JvmField
    val BMP_2 = register("bmp_2", vehicle(::Bmp2Entity).sized(3.6f, 2.1f))

    @JvmField
    val BRADLEY = register("bradley", vehicle(::BradleyEntity).sized(3.6f, 2.3f))

    @JvmField
    val ZTZ_99A = register("ztz_99a", vehicle(::Ztz99aEntity).sized(4.62f, 2.2f))

    @JvmField
    val T_90A = register("t_90a", vehicle(::T90aEntity).sized(4.62f, 2f))

    @JvmField
    val M_1A_2 = register("m_1a_2", vehicle(::M1A2Entity).sized(4.62f, 2f))

    @JvmField
    val YX_100 = register("yx_100", vehicle(::Yx100Entity).sized(5.75f, 4.0625f))

    @JvmField
    val PRISM_TANK = register("prism_tank", vehicle(::PrismTankEntity).sized(5f, 2.6f))

    @JvmField
    val PLZ_05 = register("plz_05", vehicle(::Plz05Entity).sized(4.6f, 3.25f))

    // Aircraft
    @JvmField
    val TOM_6 = register("tom_6", vehicle(::Tom6Entity).sized(1.05f, 1.0f))

    @JvmField
    val AH_6 = register("ah_6", vehicle(::Ah6Entity).sized(2.25f, 2.175f))

    @JvmField
    val MI_28 = register("mi_28", vehicle(::Mi28Entity).sized(3.375f, 3.375f))

    @JvmField
    val KV_16 = register("kv_16", vehicle(::Kv16Entity).sized(1f, 1f))

    @JvmField
    val JU_87 = register("ju_87", vehicle(::Ju87Entity).sized(3f, 2.5f))

    @JvmField
    val A_10A = register("a_10a", vehicle(::A10Entity).sized(3.375f, 2.625f))

    // Special
    @JvmField
    val DRONE = register("drone", misc(::DroneEntity).setTrackingRange(512).setUpdateInterval(1).sized(0.6f, 0.2f))

    @JvmField
    val MORTAR = register("mortar", vehicle(::MortarEntity).sized(0.8f, 1.4f))

    @JvmField
    val VEHICLE_ASSEMBLING_TABLE =
        register("vehicle_assembling_table", vehicle(::VehicleAssemblingTableVehicleEntity).sized(2f, 1.875f))

    @JvmField
    val SODAYO_PICK_UP = register("sodayo_pick_up", vehicle(::SodayoPickUpEntity).sized(2.4f, 2f))

    @JvmField
    val SODAYO_PICK_UP_HMG = register("sodayo_pick_up_hmg", vehicle(::SodayoPickUpHmgEntity).sized(2.4f, 2f))

    @JvmField
    val SODAYO_PICK_UP_ROCKET = register("sodayo_pick_up_rocket", vehicle(::SodayoPickUpRocketEntity).sized(2.4f, 2f))

    @JvmField
    val SODAYO_PICK_UP_TOW = register("sodayo_pick_up_tow", vehicle(::SodayoPickUpTowEntity).sized(2.4f, 2f))

    @JvmField
    val TRUCK = register("truck", vehicle(::TruckEntity).sized(2.6f, 3f))

    @JvmField
    val TURRET_WRECK = register("turret_wreck", vehicle(::TurretWreckEntity).sized(2.4f, 1.2f))

    private fun <T : Entity> register(
        name: String,
        entityTypeBuilder: EntityType.Builder<T>
    ): DeferredHolder<EntityType<*>, EntityType<T>> {
        return REGISTRY.register(name) { -> entityTypeBuilder.build(name) }
    }

    private fun <T : Entity> misc(
        entity: (EntityType<T>, Level) -> T
    ): EntityType.Builder<T> = EntityType.Builder.of(entity, MobCategory.MISC)

    private fun <T : Entity> vehicle(
        entity: (EntityType<T>, Level) -> T
    ): EntityType.Builder<T> = misc(entity)
        .setTrackingRange(512)
        .setUpdateInterval(1)
        .fireImmune()

    private fun <T : Entity> fastProjectile(
        entity: (EntityType<T>, Level) -> T,
        receiveVelocityUpdates: Boolean = false
    ): EntityType.Builder<T> = misc(entity)
        .setShouldReceiveVelocityUpdates(receiveVelocityUpdates)
        .setTrackingRange(64)
        .setUpdateInterval(1)
        .noSave()

    @SubscribeEvent
    fun onRegisterSpawnPlacement(event: RegisterSpawnPlacementsEvent) {
        event.register(
            SENPAI.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            { entityType, world, reason, pos, random ->
                world.difficulty != Difficulty.PEACEFUL
                        && SpawnConfig.SPAWN_SENPAI.get()
                        && Monster.isDarkEnoughToSpawn(world, pos, random)
                        && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)
            },
            RegisterSpawnPlacementsEvent.Operation.OR
        )
        event.register(
            STEEL_COIL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            { entityType, world, reason, pos, random ->
                world.difficulty != Difficulty.PEACEFUL
                        && SpawnConfig.SPAWN_STEEL_COIL.get()
                        && Monster.isDarkEnoughToSpawn(world, pos, random)
                        && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)
            },
            RegisterSpawnPlacementsEvent.Operation.OR
        )
    }

    @SubscribeEvent
    fun registerAttributes(event: EntityAttributeCreationEvent) {
        event.put(TARGET.get(), TargetEntity.createAttributes().build())
        event.put(DPS_GENERATOR.get(), DPSGeneratorEntity.createAttributes().build())
        event.put(SENPAI.get(), SenpaiEntity.createAttributes().build())
        event.put(STEEL_COIL.get(), SteelCoilEntity.createAttributes().build())
    }
}
