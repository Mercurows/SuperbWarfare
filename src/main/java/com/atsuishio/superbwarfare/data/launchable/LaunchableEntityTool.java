package com.atsuishio.superbwarfare.data.launchable;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.tools.TagDataParser;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

public class LaunchableEntityTool {
    public static Map<String, ProjectileInfo> launchableEntitiesData = CustomData.LAUNCHABLE_ENTITY;

    public static @Nullable CompoundTag getModifiedTag(ProjectileInfo projectileInfo, ShootData data) {
        JsonObject launchableData;
        if (projectileInfo.data != null) {
            launchableData = projectileInfo.data;
        } else if (launchableEntitiesData.containsKey(projectileInfo.type)) {
            launchableData = launchableEntitiesData.get(projectileInfo.type).data;
        } else {
            return null;
        }

        return TagDataParser.parse(launchableData, name -> switch (name) {
            case "@sbw:damage" -> DoubleTag.valueOf(data.damage());
            case "@sbw:owner" -> NbtUtils.createUUID(data.shooter());
            case "@sbw:owner_string_lower" ->
                    StringTag.valueOf(data.shooter().toString().replace("-", "").toLowerCase(Locale.ENGLISH));
            case "@sbw:owner_string_upper" ->
                    StringTag.valueOf(data.shooter().toString().replace("-", "").toUpperCase(Locale.ENGLISH));
            case "@sbw:explosion_damage" -> DoubleTag.valueOf(data.explosionDamage());
            case "@sbw:explosion_radius" -> DoubleTag.valueOf(data.explosionRadius());
            case "@sbw:spread" -> DoubleTag.valueOf(data.spread());
            default -> StringTag.valueOf(name);
        });
    }
}
