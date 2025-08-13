package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.DefaultDataSupplier;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
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

    private Pair<String, JsonObject> propertyOverrideCache = new Pair<>("", null);
    private boolean isOverrideValid = true;

    private final Set<VehicleProp<?>> operatingProps = new HashSet<>();

    private static final Gson GSON = DataLoader.GSON;

    @SuppressWarnings("unchecked")
    public <T> T get(VehicleProp<T> prop) {
        var modifier = prop.asModifier(this);

        if (operatingProps.contains(prop)) {
            Mod.LOGGER.warn("recursive computation for property {}", prop.name);
            return (T) DataLoader.processValue(modifier.compute());
        }

        // TODO 为什么这b玩意能为空，能不能正确初始化
        if (this.vehicle.getEntityData() == null) {
            Mod.LOGGER.warn("Entity data for vehicle entity {} is null!", this.vehicle.getType());
            return (T) DataLoader.processValue(modifier.compute());
        }

        operatingProps.add(prop);

        // property override tag
        // TODO 重写这b玩意
        var propertyOverrideString = this.vehicle.getEntityData().get(VehicleEntity.OVERRIDE);
        if (!propertyOverrideString.isEmpty()) {
            if (!propertyOverrideCache.getFirst().equals(propertyOverrideString)) {
                try {
                    propertyOverrideCache = new Pair<>(propertyOverrideString, GSON.fromJson(propertyOverrideString, JsonObject.class));
                    isOverrideValid = true;
                } catch (Exception exception) {
                    Mod.LOGGER.error("invalid property override string {}", propertyOverrideString);
                    propertyOverrideCache = new Pair<>(propertyOverrideString, new JsonObject());
                    isOverrideValid = false;
                }
            }

            var propJson = propertyOverrideCache.getSecond();
            if (propJson != null && propJson.has(prop.name) && isOverrideValid) {
                try {
                    var parsedValue = DataLoader.processValue(GSON.fromJson(propJson.get(prop.name).toString(), prop.getFieldType()));
                    modifier.apply((data, value) -> (T) parsedValue);
                } catch (Exception exception) {
                    Mod.LOGGER.error("invalid property override type for prop {}: {}", prop.name, propJson.get(prop.name).toString());
                    isOverrideValid = false;
                }
            }
        }

        operatingProps.remove(prop);
        return (T) DataLoader.processValue(modifier.compute());
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
