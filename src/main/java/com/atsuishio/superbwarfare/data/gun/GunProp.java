package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.Prop;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;

public final class GunProp<T> extends Prop<GunData, DefaultGunData, T> {

    public static final GunProp<Integer> MAX_DURABILITY = new GunProp<Integer>("MaxDurability")
            .withLimiter(v -> Math.max(0, v));

    public static final GunProp<Integer> DURABILITY_PER_SHOOT = new GunProp<Integer>("DurabilityPerShoot")
            .withLimiter(v -> Math.max(0, v));

    public static final GunProp<Double> RECOIL_X = new GunProp<>("RecoilX");
    public static final GunProp<Double> RECOIL_Y = new GunProp<>("RecoilY");
    public static final GunProp<Double> RECOIL = new GunProp<>("Recoil");

    public static final GunProp<Double> SPREAD = new GunProp<>("Spread");
    public static final GunProp<Double> DAMAGE = new GunProp<>("Damage");
    public static final GunProp<Double> HEADSHOT = new GunProp<>("Headshot");
    public static final GunProp<Double> VELOCITY = new GunProp<>("Velocity");
    public static final GunProp<Integer> MAGAZINE = new GunProp<Integer>("Magazine")
            .withLimiter((data, v) -> data.meleeOnly() ? 0 : Math.max(0, v));

    public static final GunProp<Double> MELEE_DAMAGE = new GunProp<>("MeleeDamage");
    public static final GunProp<Integer> MELEE_DURATION = new GunProp<Integer>("MeleeDuration")
            .withLimiter(v -> Math.max(1, v));

    public static final GunProp<Integer> MELEE_DAMAGE_TIME = new GunProp<Integer>("MeleeDamageTime")
            .withLimiter((data, v) -> Math.min(data.get(MELEE_DURATION) - 1, v));

    public static final GunProp<ProjectileInfo> PROJECTILE = new GunProp<>("Projectile");
    public static final GunProp<Integer> AMMO_COST_PER_SHOOT = new GunProp<Integer>("AmmoCostPerShoot")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Integer> PROJECTILE_AMOUNT = new GunProp<Integer>("ProjectileAmount")
            .withLimiter(v -> Math.max(0, v));
    public static final GunProp<Double> WEIGHT = new GunProp<>("Weight");
    public static final GunProp<FireMode> DEFAULT_FIRE_MODE = new GunProp<>("DefaultFireMode");
    public static final GunProp<Set<FireMode>> AVAILABLE_FIRE_MODES = new GunProp<>("AvailableFireModes");

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

    public static final GunProp<Double> SOUND_RADIUS = new GunProp<>("SoundRadius");

    public static final GunProp<Integer> RPM = new GunProp<Integer>("RPM")
            .withLimiter(v -> Mth.clamp(v, 1, 114514));

    public static final GunProp<Double> EXPLOSION_DAMAGE = new GunProp<>("ExplosionDamage");
    public static final GunProp<Double> EXPLOSION_RADIUS = new GunProp<>("ExplosionRadius");
    public static final GunProp<Double> GRAVITY = new GunProp<>("Gravity");

    public static final GunProp<Integer> SHOOT_DELAY = new GunProp<>("ShootDelay");
    public static final GunProp<Double> HEAT_PER_SHOOT = new GunProp<>("HeatPerShoot");

    public static final GunProp<List<String>> AVAILABLE_PERKS = new GunProp<>("AvailablePerks");

    private GunProp(String name) {
        super(DefaultGunData.class, name);
    }
}