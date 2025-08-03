package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class GunProp<T> {
    // 这b玩意必须放第一个，不然new的时候执行到props.add(this)会NPE（全恼
    private static final List<GunProp<?>> props = new ArrayList<>();

    public static final GunProp<Integer> MAX_DURABILITY = new GunProp<Integer>("MaxDurability", true)
            .withLimiter((data, v) -> Math.max(0, v));

    public static final GunProp<Double> RECOIL_X = new GunProp<>("RecoilX");
    public static final GunProp<Double> RECOIL_Y = new GunProp<>("RecoilY");
    public static final GunProp<Double> RECOIL = new GunProp<>("Recoil");

    public static final GunProp<Double> SPREAD = new GunProp<>("Spread");
    public static final GunProp<Double> DAMAGE = new GunProp<>("Damage");
    public static final GunProp<Double> HEADSHOT = new GunProp<>("Headshot");
    public static final GunProp<Double> VELOCITY = new GunProp<>("Velocity");
    public static final GunProp<Integer> MAGAZINE = new GunProp<>("Magazine");

    public static final GunProp<Double> MELEE_DAMAGE = new GunProp<>("MeleeDamage");
    public static final GunProp<Integer> MELEE_DURATION = new GunProp<Integer>("MeleeDuration")
            .withLimiter((data, v) -> Math.max(0, v));

    public static final GunProp<Integer> MELEE_DAMAGE_TIME = new GunProp<Integer>("MeleeDamageTime")
            .withLimiter((data, v) -> Math.min(data.get(MELEE_DURATION), v));

    public static final GunProp<ProjectileInfo> PROJECTILE = new GunProp<>("Projectile");
    public static final GunProp<Integer> PROJECTILE_AMOUNT = new GunProp<>("ProjectileAmount");
    public static final GunProp<Double> WEIGHT = new GunProp<>("Weight");
    public static final GunProp<FireMode> DEFAULT_FIRE_MODE = new GunProp<>("DefaultFireMode");
    public static final GunProp<Set<FireMode>> AVAILABLE_FIRE_MODES = new GunProp<>("AvailableFireModes");

    public static final GunProp<Double> DEFAULT_ZOOM = new GunProp<>("DefaultZoom");

    public static final GunProp<Integer> BURST_AMOUNT = new GunProp<>("BurstAmount");
    public static final GunProp<Double> BYPASSES_ARMOR = new GunProp<>("BypassesArmor");

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
            .withLimiter((data, v) -> Mth.clamp(v, 1, 114514));

    public static final GunProp<Double> EXPLOSION_DAMAGE = new GunProp<>("ExplosionDamage");
    public static final GunProp<Double> EXPLOSION_RADIUS = new GunProp<>("ExplosionRadius");
    public static final GunProp<Double> GRAVITY = new GunProp<>("Gravity");

    public static final GunProp<Integer> SHOOT_DELAY = new GunProp<>("ShootDelay");
    public static final GunProp<Double> HEAT_PER_SHOOT = new GunProp<>("HeatPerShoot");

    public static final GunProp<List<String>> AVAILABLE_PERKS = new GunProp<>("AvailablePerks");

    private final String name;
    private final Field field;
    private final boolean readOnly;
    public GunPropModifyContext<T> limiter;

    private GunProp(String name) {
        this(name, false);
    }

    public Type getFieldType() {
        return this.field.getType();
    }

    private GunProp(String name, boolean readOnly) {
        this.name = name;
        this.readOnly = readOnly;

        try {
            this.field = Arrays.stream(DefaultGunData.class.getFields())
                    .filter(f -> {
                        var annotation = f.getAnnotation(SerializedName.class);
                        return annotation != null && annotation.value().equals(this.name);
                    })
                    .findFirst()
                    .orElseThrow();
            this.field.setAccessible(true);
        } catch (Exception exception) {
            Mod.LOGGER.error("Could not find field {} in DefaultGunData!", name);
            throw new RuntimeException(exception);
        }

        props.add(this);
    }

    private GunProp<T> withLimiter(GunPropModifyContext<T> limiter) {
        this.limiter = limiter;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T getDefault(DefaultGunData data) {
        try {
            return (T) processValue(field.get(data));
        } catch (Exception exception) {
            Mod.LOGGER.error("Could not get field {} in DefaultGunData!", name);
            throw new RuntimeException(exception);
        }
    }

    private static Object processValue(Object value) {
        if (value instanceof ObjectToList<?> otl) {
            return otl.list.stream().map(GunProp::processValue).toList();
        } else if (value instanceof StringToObject<?> sto) {
            return processValue(sto.value);
        }
        return value;
    }

    public GunPropModifier<T> asModifier(GunData data) {
        return new GunPropModifier<>(data, this.getDefault(data.getDefault()), limiter, readOnly);
    }

    public static @Nullable GunProp<?> getByName(String name) {
        return props.stream().filter(p -> p.name.equals(name)).findFirst().orElse(null);
    }

    @FunctionalInterface
    public interface GunPropModifyContext<T> {
        T apply(@NotNull GunData data, @NotNull T value);
    }

    public static class GunPropModifier<T> {
        private final GunData data;
        private final T value;
        private final GunPropModifyContext<T> limiter;
        private final boolean readOnly;

        private final List<GunPropModifyContext<T>> modifiers = new ArrayList<>();

        private GunPropModifier(GunData data, T value, @Nullable GunPropModifyContext<T> limiter, boolean readOnly) {
            this.data = data;
            this.value = value;
            this.limiter = limiter;
            this.readOnly = readOnly;
        }

        public GunPropModifier<T> apply(@Nullable List<GunPropModifyContext<T>> modifiers) {
            if (modifiers == null || readOnly) return this;

            for (var modifier : modifiers) {
                apply(modifier);
            }
            return this;
        }

        public GunPropModifier<T> apply(@Nullable GunPropModifyContext<T> modifier) {
            if (modifier == null || readOnly) return this;

            modifiers.add(modifier);
            return this;
        }

        public GunPropModifier<T> override(@Nullable T value) {
            if (value == null || readOnly) return this;

            modifiers.add((data, v) -> value);
            return this;
        }

        public T compute() {
            if (readOnly) return value;

            var result = value;

            for (var modifier : modifiers) {
                result = modifier.apply(data, result);
            }

            if (limiter != null) {
                result = limiter.apply(data, result);
            }

            return result;
        }
    }

}