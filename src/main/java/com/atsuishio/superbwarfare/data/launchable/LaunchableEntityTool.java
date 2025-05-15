package com.atsuishio.superbwarfare.data.launchable;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.HashMap;

@EventBusSubscriber(modid = Mod.MODID)
public class LaunchableEntityTool {
    public static HashMap<String, JsonObject> launchableEntitiesData = new HashMap<>();

    /**
     * 初始化数据，从data中读取数据json文件
     */
    public static void initJsonData(ResourceManager manager) {
        launchableEntitiesData.clear();
        for (var entry : manager.listResources("launchable", file -> file.getPath().endsWith(".json")).entrySet()) {
            var attribute = entry.getValue();
            try {
                Gson gson = new Gson();
                var data = gson.fromJson(new InputStreamReader(attribute.open()), JsonObject.class);

                launchableEntitiesData.put(data.get("Type").getAsString(), data.get("Data").getAsJsonObject());
            } catch (Exception e) {
                Mod.LOGGER.error(e.getMessage());
            }
        }
    }

    public static @Nullable CompoundTag getModifiedTag(ProjectileInfo projectileInfo, ShootData data) {
        JsonObject launchableData;
        if (projectileInfo.data != null) {
            launchableData = projectileInfo.data;
        } else if (launchableEntitiesData.containsKey(projectileInfo.type)) {
            launchableData = launchableEntitiesData.get(projectileInfo.type);
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
                    StringTag.valueOf(data.shooter().toString().replace("-", "").toLowerCase());
            case "@sbw:owner_string_upper" ->
                    StringTag.valueOf(data.shooter().toString().replace("-", "").toUpperCase());
            case "@sbw:explosion_damage" -> DoubleTag.valueOf(data.explosionDamage());
            case "@sbw:explosion_radius" -> DoubleTag.valueOf(data.explosionRadius());
            case "@sbw:spread" -> DoubleTag.valueOf(data.spread());
            default -> StringTag.valueOf(value);
        };
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        initJsonData(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        initJsonData(event.getPlayerList().getServer().getResourceManager());
    }
}
