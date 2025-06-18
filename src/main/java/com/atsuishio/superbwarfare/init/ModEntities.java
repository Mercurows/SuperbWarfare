package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.SpawnConfig;
import com.atsuishio.superbwarfare.entity.*;
import com.atsuishio.superbwarfare.entity.projectile.*;
import com.atsuishio.superbwarfare.entity.vehicle.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, Mod.MODID);

    // Living Entities
    public static final DeferredHolder<EntityType<?>, EntityType<TargetEntity>> TARGET = register("target",
            EntityType.Builder.of(TargetEntity::new, MobCategory.CREATURE).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.875f, 2f));
    public static final DeferredHolder<EntityType<?>, EntityType<DPSGeneratorEntity>> DPS_GENERATOR = register("dps_generator",
            EntityType.Builder.of(DPSGeneratorEntity::new, MobCategory.CREATURE).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.875f, 2f));
    public static final DeferredHolder<EntityType<?>, EntityType<SenpaiEntity>> SENPAI = register("senpai",
            EntityType.Builder.of(SenpaiEntity::new, MobCategory.MONSTER).setTrackingRange(64).setUpdateInterval(3).eyeHeight(1.75f).sized(0.6f, 2f));

    // Misc Entities
    public static final DeferredHolder<EntityType<?>, EntityType<MortarEntity>> MORTAR = register("mortar",
            EntityType.Builder.<MortarEntity>of(MortarEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(3).eyeHeight(0.2f).fireImmune().sized(0.8f, 1.4f));
    public static final DeferredHolder<EntityType<?>, EntityType<LaserEntity>> LASER = register("laser",
            EntityType.Builder.<LaserEntity>of(LaserEntity::new, MobCategory.MISC).sized(0.1f, 0.1f).fireImmune().setUpdateInterval(1));
    public static final DeferredHolder<EntityType<?>, EntityType<FlareDecoyEntity>> FLARE_DECOY = register("flare_decoy",
            EntityType.Builder.<FlareDecoyEntity>of(FlareDecoyEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).noSave().sized(1, 1));
    public static final DeferredHolder<EntityType<?>, EntityType<SmokeDecoyEntity>> SMOKE_DECOY = register("smoke_decoy",
            EntityType.Builder.<SmokeDecoyEntity>of(SmokeDecoyEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).noSave().sized(3f, 3f));
    public static final DeferredHolder<EntityType<?>, EntityType<ClaymoreEntity>> CLAYMORE = register("claymore",
            EntityType.Builder.<ClaymoreEntity>of(ClaymoreEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));

    public static final DeferredHolder<EntityType<?>, EntityType<Blu43Entity>> BLU_43 = register("blu_43",
            EntityType.Builder.<Blu43Entity>of(Blu43Entity::new, MobCategory.MISC).setTrackingRange(32).setUpdateInterval(1).sized(0.12f, 0.05f));

    public static final DeferredHolder<EntityType<?>, EntityType<Tm62Entity>> TM_62 = register("tm_62",
            EntityType.Builder.<Tm62Entity>of(Tm62Entity::new, MobCategory.MISC).setTrackingRange(32).setUpdateInterval(1).sized(0.5f, 0.15f));
    public static final DeferredHolder<EntityType<?>, EntityType<C4Entity>> C_4 = register("c4",
            EntityType.Builder.<C4Entity>of(C4Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<WaterMaskEntity>> WATER_MASK = register("water_mask",
            EntityType.Builder.of(WaterMaskEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(3).sized(1f, 1f));

    // Projectiles
    public static final DeferredHolder<EntityType<?>, EntityType<TaserBulletEntity>> TASER_BULLET = register("taser_bullet",
            EntityType.Builder.<TaserBulletEntity>of(TaserBulletEntity::new, MobCategory.MISC).noSave().setTrackingRange(64)
                    .setUpdateInterval(1).sized(0.25f, 0.25f));

    // Fast Projectiles
    public static final DeferredHolder<EntityType<?>, EntityType<SmallCannonShellEntity>> SMALL_CANNON_SHELL = register("small_cannon_shell",
            EntityType.Builder.<SmallCannonShellEntity>of(SmallCannonShellEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.25f, 0.25f));
    public static final DeferredHolder<EntityType<?>, EntityType<RpgRocketEntity>> RPG_ROCKET = register("rpg_rocket",
            EntityType.Builder.<RpgRocketEntity>of(RpgRocketEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<MortarShellEntity>> MORTAR_SHELL = register("mortar_shell",
            EntityType.Builder.<MortarShellEntity>of(MortarShellEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<ProjectileEntity>> PROJECTILE = register("projectile",
            EntityType.Builder.<ProjectileEntity>of(ProjectileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).noSave().noSummon().sized(0.25f, 0.25f));
    public static final DeferredHolder<EntityType<?>, EntityType<CannonShellEntity>> CANNON_SHELL = register("cannon_shell",
            EntityType.Builder.<CannonShellEntity>of(CannonShellEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.75f, 0.75f));
    public static final DeferredHolder<EntityType<?>, EntityType<GunGrenadeEntity>> GUN_GRENADE = register("gun_grenade",
            EntityType.Builder.<GunGrenadeEntity>of(GunGrenadeEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<MelonBombEntity>> MELON_BOMB = register("melon_bomb",
            EntityType.Builder.<MelonBombEntity>of(MelonBombEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(1f, 1f));
    public static final DeferredHolder<EntityType<?>, EntityType<HandGrenadeEntity>> HAND_GRENADE = register("hand_grenade",
            EntityType.Builder.<HandGrenadeEntity>of(HandGrenadeEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.3f, 0.3f));
    public static final DeferredHolder<EntityType<?>, EntityType<RgoGrenadeEntity>> RGO_GRENADE = register("rgo_grenade",
            EntityType.Builder.<RgoGrenadeEntity>of(RgoGrenadeEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.3f, 0.3f));
    public static final DeferredHolder<EntityType<?>, EntityType<JavelinMissileEntity>> JAVELIN_MISSILE = register("javelin_missile",
            EntityType.Builder.<JavelinMissileEntity>of(JavelinMissileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<Agm65Entity>> AGM_65 = register("agm_65",
            EntityType.Builder.<Agm65Entity>of(Agm65Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.75f, 0.75f));
    public static final DeferredHolder<EntityType<?>, EntityType<HeliRocketEntity>> HELI_ROCKET = register("heli_rocket",
            EntityType.Builder.<HeliRocketEntity>of(HeliRocketEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<WgMissileEntity>> WG_MISSILE = register("wg_missile",
            EntityType.Builder.<WgMissileEntity>of(WgMissileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<SwarmDroneEntity>> SWARM_DRONE = register("swarm_drone",
            EntityType.Builder.<SwarmDroneEntity>of(SwarmDroneEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<Mk82Entity>> MK_82 = register("mk_82",
            EntityType.Builder.<Mk82Entity>of(Mk82Entity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(false).noSave().setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(0.8f, 0.8f));

    // Vehicles
    public static final DeferredHolder<EntityType<?>, EntityType<Mk42Entity>> MK_42 = register("mk_42",
            EntityType.Builder.of(Mk42Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(3.4f, 3.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<Hpj11Entity>> HPJ_11 = register("hpj_11",
            EntityType.Builder.of(Hpj11Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(2.8f, 2.4f));
    public static final DeferredHolder<EntityType<?>, EntityType<Mle1934Entity>> MLE_1934 = register("mle_1934",
            EntityType.Builder.of(Mle1934Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(4.5f, 2.8f));
    public static final DeferredHolder<EntityType<?>, EntityType<AnnihilatorEntity>> ANNIHILATOR = register("annihilator",
            EntityType.Builder.of(AnnihilatorEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(3).fireImmune().sized(13f, 4.2f));

    public static final DeferredHolder<EntityType<?>, EntityType<SpeedboatEntity>> SPEEDBOAT = register("speedboat",
            EntityType.Builder.of(SpeedboatEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(3.0f, 2.0f));
    public static final DeferredHolder<EntityType<?>, EntityType<WheelChairEntity>> WHEEL_CHAIR = register("wheel_chair",
            EntityType.Builder.of(WheelChairEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(1.0f, 1.0f));
    public static final DeferredHolder<EntityType<?>, EntityType<Ah6Entity>> AH_6 = register("ah_6",
            EntityType.Builder.of(Ah6Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(3.6f, 2.9f));
    public static final DeferredHolder<EntityType<?>, EntityType<Lav150Entity>> LAV_150 = register("lav_150",
            EntityType.Builder.of(Lav150Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(2.8f, 3.1f));
    public static final DeferredHolder<EntityType<?>, EntityType<Tom6Entity>> TOM_6 = register("tom_6",
            EntityType.Builder.of(Tom6Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(1.05f, 1.0f));
    public static final DeferredHolder<EntityType<?>, EntityType<Bmp2Entity>> BMP_2 = register("bmp_2",
            EntityType.Builder.of(Bmp2Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(4f, 3f));
    public static final DeferredHolder<EntityType<?>, EntityType<Yx100Entity>> YX_100 = register("yx_100",
            EntityType.Builder.of(Yx100Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(4.6f, 3.25f));

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = register("drone",
            EntityType.Builder.of(DroneEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).sized(0.6f, 0.2f));
    public static final DeferredHolder<EntityType<?>, EntityType<LaserTowerEntity>> LASER_TOWER = register("laser_tower",
            EntityType.Builder.<LaserTowerEntity>of(LaserTowerEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(0.9f, 1.65f));
    public static final DeferredHolder<EntityType<?>, EntityType<PrismTankEntity>> PRISM_TANK = register("prism_tank",
            EntityType.Builder.of(PrismTankEntity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(5f, 2.6f));
    public static final DeferredHolder<EntityType<?>, EntityType<A10Entity>> A_10A = register("a_10a",
            EntityType.Builder.of(A10Entity::new, MobCategory.MISC).setTrackingRange(64).setUpdateInterval(1).fireImmune().sized(9f, 3.5f));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String name, EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register(name, () -> entityTypeBuilder.build(name));
    }

    @SubscribeEvent
    public static void onRegisterSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.SENPAI.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, world, reason, pos, random) -> (world.getDifficulty() != Difficulty.PEACEFUL && SpawnConfig.SPAWN_SENPAI.get()
                        && Monster.isDarkEnoughToSpawn(world, pos, random) && Mob.checkMobSpawnRules(entityType, world, reason, pos, random)),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TARGET.get(), TargetEntity.createAttributes().build());
        event.put(DPS_GENERATOR.get(), DPSGeneratorEntity.createAttributes().build());
        event.put(SENPAI.get(), SenpaiEntity.createAttributes().build());
    }
}
