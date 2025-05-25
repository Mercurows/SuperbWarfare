package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
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

    public String repairMaterial() {
        return data.repairMaterial;
    }

    public float repairMaterialHealAmount() {
        return data.repairMaterialHealAmount;
    }

    public boolean canRepairManually() {
        var material = repairMaterial();
        if (material == null) return false;

        if (material.startsWith("#")) {
            material = material.substring(1);
        }
        return ResourceLocation.tryParse(material) != null;
    }

    public boolean isRepairMaterial(ItemStack stack) {
        var material = repairMaterial();
        var useTag = false;

        if (material.startsWith("#")) {
            material = material.substring(1);
            useTag = true;
        }

        var location = ResourceLocation.parse(material);
        if (!useTag) {
            return stack.getItem() == BuiltInRegistries.ITEM.get(location);
        } else {
            return stack.is(ItemTags.create(location));
        }
    }

    public float selfHurtPercent() {
        return Mth.clamp(data.selfHurtPercent, 0, 1);
    }

    public float selfHurtAmount() {
        return data.selfHurtAmount;
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

    public DamageModifier damageModifier() {
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
}
