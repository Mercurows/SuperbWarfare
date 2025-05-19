package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class VehicleData {

    public final String id;
    private final DefaultVehicleData data;

    private VehicleData(VehicleEntity entity) {
        this.id = EntityType.getKey(entity.getType()).toString();
        this.data = VehicleDataTool.vehicleData.getOrDefault(id, new DefaultVehicleData());
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

    public float maxHealth() {
        return data.maxHealth;
    }

    public int repairCooldown() {
        return data.repairCooldown;
    }

    public float repairAmount() {
        return data.repairAmount;
    }

    public int maxEnergy() {
        return data.maxEnergy;
    }

    public float upStep() {
        return data.upStep;
    }

}
