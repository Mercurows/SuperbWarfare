package com.atsuishio.superbwarfare.resource.vehicle;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class VehicleResource implements DefaultDataSupplier<DefaultVehicleResource> {

    public static final LoadingCache<VehicleEntity, VehicleResource> RESOURCE_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<>() {
                public @NotNull VehicleResource load(@NotNull VehicleEntity vehicle) {
                    return new VehicleResource(vehicle);
                }
            });

    public final VehicleEntity vehicle;
    public final String id;

    private VehicleResource(VehicleEntity vehicle) {
        this.vehicle = vehicle;
        this.id = getRegistryId(vehicle.getType());
    }

    public DefaultVehicleResource getDefault() {
        return getDefault(this.id);
    }

    public static DefaultVehicleResource getDefault(String id) {
        return CustomData.VEHICLE_RESOURCE.getOrElseGet(id, DefaultVehicleResource::new);
    }

    public static DefaultVehicleResource getDefault(VehicleEntity vehicle) {
        return getDefault(vehicle.getType());
    }

    public static DefaultVehicleResource getDefault(EntityType<?> type) {
        return getDefault(getRegistryId(type));
    }

    public static VehicleResource from(VehicleEntity stack) {
        return RESOURCE_CACHE.getUnchecked(stack);
    }

    public static String getRegistryId(EntityType<?> type) {
        return EntityType.getKey(type).toString();
    }
}
