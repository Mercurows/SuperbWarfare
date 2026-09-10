package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class VehicleData implements DefaultDataSupplier<DefaultVehicleData> {

    public final String id;
    public final VehicleEntity vehicle;

    private VehicleData(VehicleEntity entity) {
        this.id = getRegistryId(entity.getType());
        this.vehicle = entity;
    }

    private final JsonPropertyModifier<VehicleData, DefaultVehicleData> jsonPropModifier = new JsonPropertyModifier<>();

    public static @NotNull DefaultVehicleData compute(VehicleEntity vehicle) {
        return from(vehicle).compute();
    }

    private DefaultVehicleData cache = null;

    public DefaultVehicleData compute() {
        if (cache != null) return cache;

        var overrideString = vehicle.isInitialized() ? vehicle.getOverride() : null;

        if (overrideString == null || overrideString.isEmpty()) {
            // Fast path: no per-vehicle override, so the shared datapack default *is* the result.
            // It is clamped once when the datapack is loaded (see CustomData.VEHICLE_DATA), which
            // makes it safe to hand out read-only without the previous GSON deep copy.
            cache = getDefault();
            return cache;
        }

        var raw = getDefault().copy();

        jsonPropModifier.update(overrideString);
        raw = jsonPropModifier.computeProperties(this, raw);

        raw.limit();
        cache = raw;

        return raw;
    }

    /**
     * Fully qualified {@link CustomData#GUN_DATA} id of one vehicle weapon's baseline:
     * {@code <vehicleId>.<weaponKey>} (e.g. {@code superbwarfare:bmp_2.Cannon}).
     */
    public static String weaponDefaultDataId(String vehicleId, String weaponKey) {
        return "vehicle:" + vehicleId + "." + weaponKey;
    }

    /**
     * Registers every vehicle weapon's parsed {@link DefaultGunData} into the given gun data map under
     * {@link #weaponDefaultDataId(String, String)}.
     *
     * <p>Vehicle-mounted weapons all share the single {@code superbwarfare:vehicle_gun} item id, so they
     * cannot resolve their baseline from the item. Registering the per-vehicle weapon baselines lets a
     * GunData resolve its baseline from its own stack instead of an injected default-data supplier.
     *
     * @param gunDataMap the raw {@link CustomData#GUN_DATA} map, which must already have been (re)loaded.
     */
    @SuppressWarnings("unchecked")
    public static void registerWeaponDefaults(Map<String, ?> gunDataMap) {
        var target = (Map<String, Object>) gunDataMap;

        for (var vehicleData : CustomData.VEHICLE_DATA.values()) {
            for (var weapon : vehicleData.weapons().entrySet()) {
                var id = weaponDefaultDataId(vehicleData.getId(), weapon.getKey());
                weapon.getValue().setId(id);
                target.put(id, weapon.getValue());
            }
        }
    }

    public void update() {
        this.cache = null;
    }

    public static DefaultVehicleData getDefault(String id) {
        var isDefault = !CustomData.VEHICLE_DATA.containsKey(id);
        var data = CustomData.VEHICLE_DATA.getOrElseGet(id, DefaultVehicleData::new);
        data.isDefaultData = isDefault;
        return data;
    }

    public DefaultVehicleData getDefault() {
        return getDefault(this.id);
    }

    public static DefaultVehicleData getDefault(VehicleEntity entity) {
        return getDefault(entity.getType());
    }

    public static DefaultVehicleData getDefault(EntityType<?> type) {
        return getDefault(getRegistryId(type));
    }

    public static String getRegistryId(EntityType<?> type) {
        return EntityType.getKey(type).toString();
    }

    public static final LoadingCache<VehicleEntity, VehicleData> dataCache = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(new CacheLoader<>() {
                public @NotNull VehicleData load(@NotNull VehicleEntity entity) {
                    return new VehicleData(entity);
                }
            });

    public static @NotNull VehicleData from(VehicleEntity entity) {
        return dataCache.getUnchecked(entity);
    }

    @SuppressWarnings("unchecked")
    public DamageModifier damageModifier() {
        var modifier = new DamageModifier();
        var data = compute();

        if (data.applyDefaultDamageModifiers) {
            modifier.addAll(DamageModifier.createDefaultModifier().toList());
            modifier.reduce(5, ModDamageTypes.VEHICLE_STRIKE);
        }

        return modifier.addAll((List<DamageModify>) DataLoader.processValue(data.damageModifiers));
    }
}
