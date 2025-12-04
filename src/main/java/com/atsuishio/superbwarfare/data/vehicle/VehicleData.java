package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.data.JsonPropertyModifier;
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

        var raw = getDefault().copy();

        if (vehicle.isInitialized()) {
            jsonPropModifier.update(this.vehicle.getOverride());
            raw = jsonPropModifier.computeProperties(this, raw);
        }

        raw.limit();
        cache = raw;

        return raw;
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
