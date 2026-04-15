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

public class LaunchableEntityTool {

    public static @Nullable CompoundTag getModifiedTag(ProjectileInfo projectileInfo, ShootData data) {
        JsonObject launchableData;
        if (projectileInfo.data != null) {
            launchableData = projectileInfo.data;
        } else if (CustomData.LAUNCHABLE_ENTITY.containsKey(projectileInfo.getId())) {
            launchableData = CustomData.LAUNCHABLE_ENTITY.get(projectileInfo.getId()).data;
        } else {
            return null;
        }

        return TagDataParser.parse(launchableData, name -> switch (name) {
            case "@sbw:damage" -> DoubleTag.valueOf(data.damage());
            case "@sbw:owner" -> data.shooter() != null ? NbtUtils.createUUID(data.shooter()) : null;
            case "@sbw:owner_string_lower" -> data.shooter() != null
                    ? StringTag.valueOf(data.shooter().toString().replace("-", "").toLowerCase(Locale.ROOT))
                    : null;
            case "@sbw:owner_string_upper" -> data.shooter() != null
                    ? StringTag.valueOf(data.shooter().toString().replace("-", "").toUpperCase(Locale.ROOT))
                    : null;
            case "@sbw:explosion_damage" -> DoubleTag.valueOf(data.explosionDamage());
            case "@sbw:explosion_radius" -> DoubleTag.valueOf(data.explosionRadius());
            case "@sbw:spread" -> DoubleTag.valueOf(data.spread());
            default -> StringTag.valueOf(name);
        });
    }
}
