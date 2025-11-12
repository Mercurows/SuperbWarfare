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

    public static final LoadingCache<Mob, MobGunData> dataCache = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(new CacheLoader<>() {
                public @NotNull MobGunData load(@NotNull Mob mob) {
                    return new MobGunData(mob);
                }
            });

    public final DefaultMobGunData data;
    private final Mob mob;
    private GunData gunData;
    private GunSpawnData selectedData;

    private MobGunData(Mob mob) {
        this.mob = mob;
        this.data = CustomData.MOB_GUNS.get(EntityType.getKey(mob.getType()).toString());
    }

    public static @Nullable MobGunData from(Mob mob) {
        var mobId = EntityType.getKey(mob.getType()).toString();
        if (!CustomData.MOB_GUNS.containsKey(mobId)) return null;

        return dataCache.getUnchecked(mob);
    }

    public @Nullable GunData getGunData() {
        if (this.gunData != null) return gunData;

        var guns = this.data.guns.list.stream().map(d -> d.value).toList();
        var totalWeight = guns.stream().mapToInt(g -> Math.max(0, g.weight)).sum();

        if (totalWeight <= 0) {
            return null;
        }

        var random = this.mob.level().random.nextInt(totalWeight);

        int currentWeight = 0;
        for (var gun : guns) {
            currentWeight += gun.weight;
            if (random < currentWeight) {
                this.selectedData = gun;
                break;
            }
        }

        if (this.selectedData == null) {
            return null;
        }

        var gunID = selectedData.id;
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

        if (selectedData.data != null) {
            NBTTool.saveTag(stack, TagDataParser.parse(selectedData.data));
        }

        var data = GunData.from(stack);

        if (selectedData.override != null) {
            data.propertyOverrideString.set(DataLoader.GSON.toJson(selectedData.override));
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
        return selectedData.spawnWithLoadedAmmo;
    }

    public double shootDistance() {
        return selectedData.shootDistance;
    }

    public int backupAmmoCount() {
        return selectedData.backupAmmo;
    }

    public int aimTime() {
        return Math.max(0, selectedData.aimTime);
    }

    public long semiFireInterval() {
        return Math.max(0, selectedData.semiFireInterval);
    }

    public boolean clearAimTimeWhenLostSight() {
        return selectedData.clearAimTimeWhenLostSight;
    }

    public boolean zoom() {
        return selectedData.zoom;
    }

    public double spread() {
        return selectedData.spread;
    }
}
