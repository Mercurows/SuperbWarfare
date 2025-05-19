package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.Gson;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

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

                if (!vehicleData.containsKey(data.id)) {
                    vehicleData.put(data.id, data);
                }
            } catch (Exception e) {
                Mod.LOGGER.error(e.getMessage());
            }
        }
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