package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.message.receive.VehiclesDataMessage;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.InputStreamReader;
import java.util.HashMap;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
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

            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), VehiclesDataMessage.create());
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        initJsonData(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        var players = event.getPlayerList();
        var server = players.getServer();
        initJsonData(server.getResourceManager());

        var message = VehiclesDataMessage.create();
        for (var player : players.getPlayers()) {
            if (server.isSingleplayerOwner(player.getGameProfile())) {
                continue;
            }

            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}