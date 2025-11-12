package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.ModColor;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;

@Deprecated(forRemoval = true)
@SuppressWarnings({"unused", "removal"})
public final class GunProp<T> extends com.atsuishio.superbwarfare.data.Prop<GunData, DefaultGunData, T> {

    public static final GunProp<Integer> MAX_DURABILITY = new GunProp<Integer>("MaxDurability")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Integer> DURABILITY_PER_SHOOT = new GunProp<Integer>("DurabilityPerShoot")
            .withLimiter(v -> Math.max(0, v));

    public static final GunProp<Integer> MAX_ENERGY = new GunProp<Integer>("MaxEnergy")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Integer> MAX_RECEIVE_ENERGY = new GunProp<Integer>("MaxReceiveEnergy")
            .withLimiter((m, d, v) -> {
                var maxEnergy = m.get(MAX_ENERGY);
                var value = Mth.clamp(v, -1, m.get(MAX_ENERGY));
                return value < 0 ? maxEnergy : value;
            });
    public static final GunProp<Integer> MAX_EXTRACT_ENERGY = new GunProp<Integer>("MaxExtractEnergy")
            .withLimiter((m, d, v) -> {
                var maxEnergy = m.get(MAX_ENERGY);
                var value = Mth.clamp(v, -1, m.get(MAX_ENERGY));
                return value < 0 ? maxEnergy : value;
            });

    public static final GunProp<Double> RECOIL_X = new GunProp<>("RecoilX");
    public static final GunProp<Double> RECOIL_Y = new GunProp<>("RecoilY");
    public static final GunProp<Double> RECOIL = new GunProp<>("Recoil");

    public static final GunProp<Integer> RECOIL_TIME = new GunProp<>("RecoilTime");
    public static final GunProp<Float> RECOIL_FORCE = new GunProp<>("RecoilForce");
    public static final GunProp<Double> SPREAD = new GunProp<>("Spread");
    public static final GunProp<Double> DAMAGE = new GunProp<>("Damage");
    public static final GunProp<Double> HEADSHOT = new GunProp<>("Headshot");
    public static final GunProp<Double> VELOCITY = new GunProp<>("Velocity");
    public static final GunProp<Double> MELEE_DAMAGE = new GunProp<>("MeleeDamage");
    public static final GunProp<Integer> MELEE_DURATION = new GunProp<Integer>("MeleeDuration")
            .withLimiter(v -> Math.max(1, v));
    public static final GunProp<Double> ZOOM_SPREAD_RATE = new GunProp<Double>("ZoomSpreadRate")
            .withLimiter(v -> Mth.clamp(v, 0, 1));

    public static final GunProp<Integer> RANGE = new GunProp<Integer>("Range")
            .withLimiter(v -> Math.max(1, v));

    public static final GunProp<Integer> MELEE_DAMAGE_TIME = new GunProp<Integer>("MeleeDamageTime")
            .withLimiter((m, d, v) -> Math.min(m.get(MELEE_DURATION) - 1, v));

    public static final GunProp<ProjectileInfo> PROJECTILE = new GunProp<>("Projectile");
    public static final GunProp<Integer> AMMO_COST_PER_SHOOT = new GunProp<Integer>("AmmoCostPerShoot")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Integer> PROJECTILE_AMOUNT = new GunProp<Integer>("ProjectileAmount")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Double> WEIGHT = new GunProp<Double>("Weight")
            .withLimiter(v -> Math.max(1, v));
    public static final GunProp<String> DEFAULT_FIRE_MODE = new GunProp<>("DefaultFireMode");
    public static final GunProp<List<FireModeInfo>> AVAILABLE_FIRE_MODES = new GunProp<List<FireModeInfo>>("AvailableFireModes")
            .withSupplier(DefaultGunData::getFireModes);

    public static final GunProp<Integer> MAGAZINE = new GunProp<Integer>("Magazine")
            .withLimiter((m, d, v) -> (m.get(PROJECTILE_AMOUNT) <= 0 && m.get(MELEE_DAMAGE) > 0) ? 0 : Math.max(0, v));

    public static final GunProp<Set<ReloadType>> RELOAD_TYPES = new GunProp<Set<ReloadType>>("ReloadTypes")
            .whenNull(Set.of());

    public static final GunProp<SeekType> SEEK_TYPE = new GunProp<SeekType>("SeekType")
            .whenNull(SeekType.NONE);

    public static final GunProp<GunType> GUN_TYPE = new GunProp<>("GunType");

    public static final GunProp<Boolean> AUTO_RELOAD = new GunProp<>("AutoReload");
    public static final GunProp<Integer> AUTO_ITERATIVE_RELOAD_TIME = new GunProp<Integer>("AutoIterativeReloadTime")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Boolean> WITHDRAW_AMMO_WHEN_CHANGE_SLOT = new GunProp<>("WithdrawAmmoWhenChangeSlot");

    public static final GunProp<Boolean> ZOOM_RELOAD = new GunProp<>("ZoomReload");
    public static final GunProp<Boolean> CLEAR_HOLD_PROGRESS_AFTER_SHOOT = new GunProp<>("ClearHoldProgressAfterShoot");

    public static final GunProp<Double> DEFAULT_ZOOM = new GunProp<>("DefaultZoom");

    public static final GunProp<Integer> BURST_AMOUNT = new GunProp<Integer>("BurstAmount")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Double> BYPASSES_ARMOR = new GunProp<>("BypassesArmor");

    public static final GunProp<List<AmmoConsumer>> AMMO_CONSUMER = new GunProp<List<AmmoConsumer>>("AmmoType")
            .withSupplier(DefaultGunData::getAmmoConsumers);

    public static final GunProp<Integer> NORMAL_RELOAD_TIME = new GunProp<>("NormalReloadTime");
    public static final GunProp<Integer> EMPTY_RELOAD_TIME = new GunProp<>("EmptyReloadTime");
    public static final GunProp<Integer> BOLT_ACTION_TIME = new GunProp<>("BoltActionTime");
    public static final GunProp<Integer> PREPARE_TIME = new GunProp<>("PrepareTime");
    public static final GunProp<Integer> PREPARE_LOAD_TIME = new GunProp<>("PrepareLoadTime");
    public static final GunProp<Integer> PREPARE_AMMO_LOAD_TIME = new GunProp<>("PrepareAmmoLoadTime");
    public static final GunProp<Integer> PREPARE_EMPTY_TIME = new GunProp<>("PrepareEmptyTime");
    public static final GunProp<Integer> ITERATIVE_TIME = new GunProp<>("IterativeTime");
    public static final GunProp<Integer> ITERATIVE_AMMO_LOAD_TIME = new GunProp<>("IterativeAmmoLoadTime");
    public static final GunProp<Integer> ITERATIVE_LOAD_AMOUNT = new GunProp<>("IterativeLoadAmount");
    public static final GunProp<Integer> FINISH_TIME = new GunProp<>("FinishTime");
    public static final GunProp<Integer> BURST_COOLDOWN = new GunProp<>("BurstCooldown");
    public static final GunProp<Integer> SHOOT_ANIMATION_TIME = new GunProp<>("ShootAnimationTime");
    public static final GunProp<Integer> ANTI_AIR_PROJECTILE = new GunProp<>("AntiAirProjectile");
    public static final GunProp<Boolean> AUTO_LOAD_WHILE_EMPTY = new GunProp<>("AutoLoadWhileEmpty");

    public static final GunProp<Double> SOUND_RADIUS = new GunProp<>("SoundRadius");

    public static final GunProp<Integer> RPM = new GunProp<Integer>("RPM")
            .withLimiter(v -> Mth.clamp(v, 1, 114514));

    public static final GunProp<Double> EXPLOSION_DAMAGE = new GunProp<>("ExplosionDamage");
    public static final GunProp<Double> EXPLOSION_RADIUS = new GunProp<>("ExplosionRadius");
    public static final GunProp<Double> GRAVITY = new GunProp<>("Gravity");

    public static final GunProp<Integer> SHOOT_DELAY = new GunProp<>("ShootDelay");
    public static final GunProp<Double> HEAT_PER_SHOOT = new GunProp<>("HeatPerShoot");
    public static final GunProp<Double> NATURAL_COOLDOWN = new GunProp<>("NaturalCooldown");
    public static final GunProp<Double> IN_WATER_COOLDOWN_RATE = new GunProp<>("InWaterCooldownRate");
    public static final GunProp<Double> IN_SNOW_COOLDOWN_RATE = new GunProp<>("InSnowCooldownRate");
    public static final GunProp<Double> IN_FIRE_COOLDOWN_RATE = new GunProp<>("InFireCooldownRate");
    public static final GunProp<Double> IN_LAVA_COOLDOWN_RATE = new GunProp<>("InLavaCooldownRate");

    public static final GunProp<List<String>> AVAILABLE_PERKS = new GunProp<>("AvailablePerks");

    public static final GunProp<Integer> SEEK_TIME = new GunProp<>("SeekTime");
    public static final GunProp<Double> SEEK_ANGLE = new GunProp<>("SeekAngle");
    public static final GunProp<Double> SEEK_RANGE = new GunProp<>("SeekRange");

    // TODO 这几个换到 gun assets里面
    public static final GunProp<String> ICON = new GunProp<>("Icon");
    public static final GunProp<String> CROSSHAIR = new GunProp<>("Crosshair");
    public static final GunProp<ModColor> CROSSHAIR_COLOR = new GunProp<>("CrosshairColor");
    public static final GunProp<String> NAME = new GunProp<>("Name");

    public static final GunProp<ShootPos> SHOOT_POS = new GunProp<>("ShootPos");
    public static final GunProp<SoundInfo> SOUND_INFO = new GunProp<>("SoundInfo");


    private GunProp(String name) {
        super(DefaultGunData.class, name);
    }
}