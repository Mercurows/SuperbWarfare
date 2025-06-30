package com.atsuishio.superbwarfare.data.launchable;

import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
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

        var tag = new CompoundTag();

        for (var d : launchableData.entrySet()) {
            var parsed = parseData(d.getValue(), data);
            if (parsed == null) continue;
            tag.put(d.getKey(), parsed);
        }

        return tag;
    }

    private static @Nullable Tag parseData(JsonElement object, ShootData data) {
        if (object.isJsonObject()) {
            var tag = new CompoundTag();
            for (var d : object.getAsJsonObject().entrySet()) {
                var parsed = parseData(d.getValue(), data);
                if (parsed == null) continue;
                tag.put(d.getKey(), parsed);
            }
            return tag;
        } else if (object.isJsonArray()) {
            var tag = new ListTag();
            for (var d : object.getAsJsonArray()) {
                tag.add(parseData(d, data));
            }
            return tag;
        } else if (object.isJsonPrimitive()) {
            var prime = object.getAsJsonPrimitive();
            if (prime.isString()) {
                return modifyStringTag(prime.getAsString(), data);
            } else if (prime.isNumber()) {
                return DoubleTag.valueOf(prime.getAsLong());
            } else if (prime.isBoolean()) {
                return ByteTag.valueOf(prime.getAsBoolean());
            }
            return null;
        }
        return null;
    }

    private static Tag modifyStringTag(String value, ShootData data) {
        return switch (value) {
            case "@sbw:damage" -> DoubleTag.valueOf(data.damage());
            case "@sbw:owner" -> NbtUtils.createUUID(data.shooter());
            case "@sbw:owner_string_lower" ->
                    StringTag.valueOf(data.shooter().toString().replace("-", "").toLowerCase(Locale.ENGLISH));
            case "@sbw:owner_string_upper" ->
                    StringTag.valueOf(data.shooter().toString().replace("-", "").toUpperCase(Locale.ENGLISH));
            case "@sbw:explosion_damage" -> DoubleTag.valueOf(data.explosionDamage());
            case "@sbw:explosion_radius" -> DoubleTag.valueOf(data.explosionRadius());
            case "@sbw:spread" -> DoubleTag.valueOf(data.spread());
            default -> StringTag.valueOf(value);
        };
    }
}
