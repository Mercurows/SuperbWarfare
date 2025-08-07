package com.atsuishio.superbwarfare.data.mob_guns;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.TagDataParser;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobGunData {

    // TODO 这里不应该这么缓存，修改为正确的处理方法
    public static final LoadingCache<String, MobGunData> dataCache = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<>() {
                public @NotNull MobGunData load(@NotNull String id) {
                    return new MobGunData(id);
                }
            });

    public final boolean isDefault;
    public final DefaultMobGunData data;
    private GunData gunData;

    private MobGunData(String id) {
        this.isDefault = CustomData.MOB_GUNS.containsKey(id);
        this.data = CustomData.MOB_GUNS.getOrDefault(id, new DefaultMobGunData());
    }

    public static MobGunData from(Mob entity) {
        return from(entity.getType());
    }

    public static MobGunData from(EntityType<?> type) {
        return dataCache.getUnchecked(EntityType.getKey(type).toString());
    }

    public @Nullable GunData getGunData() {
        if (this.gunData != null) return gunData;

        var gunID = this.data.gunID;

        var location = ResourceLocation.tryParse(gunID);
        if (location == null) {
            Mod.LOGGER.warn("invalid gun id: {}", gunID);
            return null;
        }

        var item = BuiltInRegistries.ITEM.get(location);
        if (item == Items.AIR || !(item instanceof GunItem)) {
            Mod.LOGGER.warn("invalid gun item {} for id {}", item, gunID);
            return null;
        }

        var stack = new ItemStack(item);

        if (data.data != null) {
            NBTTool.saveTag(stack, TagDataParser.parse(data.data));
        }

        var data = GunData.from(stack);

        if (this.data.override != null) {
            data.propertyOverrideString.set(DataLoader.GSON.toJson(this.data.override));
        }
        data.save();
        this.gunData = data;

        return data;
    }

    public int goalWeight() {
        return data.goalWeight;
    }

    public double probability() {
        return Mth.clamp(data.probability, 0, 1);
    }

    public boolean spawnWithLoadedAmmo() {
        return data.spawnWithLoadedAmmo;
    }

    public double shootDistance() {
        return data.shootDistance;
    }

    public int backupAmmoCount() {
        return data.backupAmmo;
    }
}
