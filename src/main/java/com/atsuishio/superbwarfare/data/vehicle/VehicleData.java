package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.data.JsonPropModifier;
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

    private final JsonPropModifier<VehicleData, DefaultVehicleData> jsonPropModifier = new JsonPropModifier<>();

    public static DefaultVehicleData compute(VehicleEntity vehicle) {
        return from(vehicle).compute();
    }

    public DefaultVehicleData compute() {
        var raw = getDefault().copy();

        jsonPropModifier.update(this.vehicle.getEntityData().get(VehicleEntity.OVERRIDE));
        raw = jsonPropModifier.compute(this, raw);

        raw.limit();
        return raw;
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
            .build(new CacheLoader<>() {
                public @NotNull VehicleData load(@NotNull VehicleEntity entity) {
                    return new VehicleData(entity);
                }
            });

    public static VehicleData from(VehicleEntity entity) {
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
