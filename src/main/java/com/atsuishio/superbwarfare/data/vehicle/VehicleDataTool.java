package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.message.receive.VehiclesDataMessage;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.InputStreamReader;
import java.util.HashMap;

@EventBusSubscriber(modid = Mod.MODID)
public class VehicleDataTool {
    public static HashMap<String, DefaultVehicleData> vehicleData = new HashMap<>();

    public static final String VEHICLE_DATA_FOLDER = "vehicles";

    public static void initJsonData(ResourceManager manager) {
        vehicleData.clear();
        VehicleData.dataCache.invalidateAll();

        for (var entry : manager.listResources(VEHICLE_DATA_FOLDER, file -> file.getPath().endsWith(".json")).entrySet()) {
            var attribute = entry.getValue();

            try {
                Gson gson = new Gson();
                var data = gson.fromJson(new InputStreamReader(attribute.open()), DefaultVehicleData.class);

                String id;
                if (!data.id.isEmpty()) {
                    id = data.id;
                } else {
                    var path = entry.getKey().getPath();
                    id = Mod.MODID + ":" + path.substring(VEHICLE_DATA_FOLDER.length() + 1, path.length() - VEHICLE_DATA_FOLDER.length() - 1);
                    Mod.LOGGER.warn("Vehicle ID for {} is empty, try using {} as id", id, path);
                    data.id = id;
                }

                if (!vehicleData.containsKey(id)) {
                    vehicleData.put(id, data);
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

            PacketDistributor.sendToPlayer(player, VehiclesDataMessage.create());
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

        var message = VehiclesDataMessage.create();
        for (var player : event.getRelevantPlayers().toList()) {
            if (server.isSingleplayerOwner(player.getGameProfile())) {
                continue;
            }

            PacketDistributor.sendToPlayer(player, message);
        }
    }
}