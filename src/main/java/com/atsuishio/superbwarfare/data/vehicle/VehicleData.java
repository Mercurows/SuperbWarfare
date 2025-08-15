package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.data.StringPropModifier;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class VehicleData implements DefaultDataSupplier<DefaultVehicleData> {

    public final String id;
    public final VehicleEntity vehicle;

    private VehicleData(VehicleEntity entity) {
        this.id = getRegistryId(entity.getType());
        this.vehicle = entity;
    }

    private final Set<VehicleProp<?>> operatingProps = new HashSet<>();

    private final StringPropModifier<VehicleData, DefaultVehicleData> stringPropModifier = new StringPropModifier<>();

    public <T> T get(VehicleProp<T> prop) {
        var modifier = prop.asModifier(this);

        if (operatingProps.contains(prop)) {
            Mod.LOGGER.warn("recursive computation for property {}", prop.name);
            return modifier.compute();
        }

        operatingProps.add(prop);

        if (this.vehicle.isInitialized()) {
            // vehicle modifiers
            modifier.apply(this.vehicle.getModifier(prop));

            // property override tag
            var propertyOverrideString = this.vehicle.getEntityData().get(VehicleEntity.OVERRIDE);
            stringPropModifier.modifyPropertyByString(propertyOverrideString, prop);
            modifier.apply(stringPropModifier.getModifier(prop));
        }

        operatingProps.remove(prop);
        return modifier.compute();
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

    public boolean canRepairManually() {
        var material = get(VehicleProp.REPAIR_MATERIAL);
        if (material == null) return false;

        if (material.startsWith("#")) {
            material = material.substring(1);
        }
        return ResourceLocation.tryParse(material) != null;
    }

    public boolean isRepairMaterial(ItemStack stack) {
        var material = get(VehicleProp.REPAIR_MATERIAL);
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

    public DamageModifier damageModifier() {
        var modifier = new DamageModifier();

        if (get(VehicleProp.APPLY_DEFAULT_DAMAGE_MODIFIERS)) {
            modifier.addAll(DamageModifier.createDefaultModifier().toList());
            modifier.reduce(5, ModDamageTypes.VEHICLE_STRIKE);
        }

        return modifier.addAll(get(VehicleProp.DAMAGE_MODIFIERS));
    }
}
