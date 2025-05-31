package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.network.message.receive.GunsDataMessage;
import com.google.gson.Gson;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.UUID;

@EventBusSubscriber(modid = Mod.MODID)
public class GunsTool {

    public static HashMap<String, DefaultGunData> gunsData = new HashMap<>();

    public static final String GUN_DATA_FOLDER = "guns";

    /**
     * 初始化数据，从data中读取数据json文件
     */
    public static void initJsonData(ResourceManager manager) {
        gunsData.clear();
        GunData.dataCache.invalidateAll();

        for (var entry : manager.listResources(GUN_DATA_FOLDER, file -> file.getPath().endsWith(".json")).entrySet()) {
            var attribute = entry.getValue();

            try {
                Gson gson = new Gson();
                var data = gson.fromJson(new InputStreamReader(attribute.open()), DefaultGunData.class);

                String id;
                if (!data.id.trim().isEmpty()) {
                    id = data.id;
                } else {
                    var path = entry.getKey().getPath();
                    id = Mod.MODID + ":" + path.substring(GUN_DATA_FOLDER.length() + 1, path.length() - GUN_DATA_FOLDER.length() - 1);
                    Mod.LOGGER.warn("Gun ID for {} is empty, try using {} as id", path, id);
                    data.id = id;
                }

                if (!gunsData.containsKey(id)) {
                    gunsData.put(id, data);
                }
            } catch (Exception e) {
                Mod.LOGGER.error(e.getMessage());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var server = player.getServer();
            if (server != null && server.isSingleplayerOwner(player.getGameProfile())) {
                return;
            }

            PacketDistributor.sendToPlayer(player, GunsDataMessage.create());
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        initJsonData(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();
        initJsonData(server.getResourceManager());

        var message = GunsDataMessage.create();
        for (var player : event.getRelevantPlayers().toList()) {
            if (server.isSingleplayerOwner(player.getGameProfile())) {
                continue;
            }

            PacketDistributor.sendToPlayer(player, message);
        }
    }

    public static void setGunIntTag(final CompoundTag tag, String name, int num) {
        var data = tag.getCompound("GunData");
        data.putInt(name, num);
        tag.put("GunData", data);
    }

    public static int getGunIntTag(final CompoundTag tag, String name) {
        return getGunIntTag(tag, name, 0);
    }

    public static int getGunIntTag(final CompoundTag tag, String name, int defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getInt(name);
    }

    public static double getGunDoubleTag(final CompoundTag tag, String name) {
        return getGunDoubleTag(tag, name, 0);
    }

    public static double getGunDoubleTag(final CompoundTag tag, String name, double defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getDouble(name);
    }

    @Nullable
    public static UUID getGunUUID(final CompoundTag tag) {
        if (!tag.contains("GunData")) return null;

        CompoundTag data = tag.getCompound("GunData");
        if (!data.hasUUID("UUID")) return null;
        return data.getUUID("UUID");
    }
}
