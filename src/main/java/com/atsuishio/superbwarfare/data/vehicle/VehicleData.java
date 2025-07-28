package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class VehicleData {

    public final String id;
    public final VehicleEntity vehicle;

    private VehicleData(VehicleEntity entity) {
        this.id = getRegistryId(entity.getType());
        this.vehicle = entity;
    }

    public static DefaultVehicleData getDefault(String id) {
        var isDefault = !VehicleDataTool.vehicleData.containsKey(id);
        var data = VehicleDataTool.vehicleData.getOrDefault(id, new DefaultVehicleData());
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

    public float maxHealth() {
        return getDefault().maxHealth;
    }

    public int repairCooldown() {
        return getDefault().repairCooldown;
    }

    public float repairAmount() {
        return getDefault().repairAmount;
    }

    public String repairMaterial() {
        return getDefault().repairMaterial;
    }

    public float repairMaterialHealAmount() {
        return getDefault().repairMaterialHealAmount;
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

        var location = Objects.requireNonNull(ResourceLocation.tryParse(material));
        if (!useTag) {
            return stack.getItem() == ForgeRegistries.ITEMS.getValue(location);
        } else {
            return stack.is(ItemTags.create(location));
        }
    }

    public float selfHurtPercent() {
        return Mth.clamp(getDefault().selfHurtPercent, 0, 1);
    }

    public float selfHurtAmount() {
        return getDefault().selfHurtAmount;
    }

    public int maxEnergy() {
        return getDefault().maxEnergy;
    }

    public float upStep() {
        return getDefault().upStep;
    }

    public boolean allowFreeCam() {
        return getDefault().allowFreeCam;
    }

    public float mass() {
        return getDefault().mass;
    }

    public DamageModifier damageModifier() {
        var modifier = new DamageModifier();

        if (getDefault().applyDefaultDamageModifiers) {
            modifier.addAll(DamageModifier.createDefaultModifier().toList());
            modifier.reduce(5, ModDamageTypes.VEHICLE_STRIKE);
        }

        return modifier.addAll(getDefault().damageModifiers.list);
    }
}
