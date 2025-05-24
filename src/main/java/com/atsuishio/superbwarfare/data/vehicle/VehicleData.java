package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class VehicleData {

    public final String id;
    public final DefaultVehicleData data;
    public final VehicleEntity vehicle;

    private VehicleData(VehicleEntity entity) {
        this.id = EntityType.getKey(entity.getType()).toString();
        this.data = VehicleDataTool.vehicleData.getOrDefault(id, new DefaultVehicleData());
        this.vehicle = entity;
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

    public boolean allowFreeCam() {
        return data.allowFreeCam;
    }

    public float mass() {
        return data.mass;
    }

    public static final LoadingCache<String, DamageModifier> damageModifiers = CacheBuilder.newBuilder()
            .build(new CacheLoader<>() {
                public @NotNull DamageModifier load(@NotNull String id) {
                    var data = VehicleDataTool.vehicleData.getOrDefault(id, new DefaultVehicleData());
                    var modifier = new DamageModifier();

                    if (data.applyDefaultDamageModifiers) {
                        modifier.immuneTo(EntityType.POTION)
                                .immuneTo(EntityType.AREA_EFFECT_CLOUD)
                                .immuneTo(DamageTypes.FALL)
                                .immuneTo(DamageTypes.DROWN)
                                .immuneTo(DamageTypes.DRAGON_BREATH)
                                .immuneTo(DamageTypes.WITHER)
                                .immuneTo(DamageTypes.WITHER_SKULL)
                                .reduce(5, ModDamageTypes.VEHICLE_STRIKE);
                    }

                    return modifier.addAll(data.damageModifiers);
                }
            });

    public DamageModifier damageModifier() {
        return damageModifiers.getUnchecked(id);
    }

}
