package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.network.message.receive.VehiclesDataMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;

@EventBusSubscriber(modid = Mod.MODID)
public class VehicleDataTool {

    public static HashMap<String, DefaultVehicleData> vehicleData = CustomData.VEHICLE;

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
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        var server = event.getPlayerList().getServer();

        var message = VehiclesDataMessage.create();
        for (var player : event.getRelevantPlayers().toList()) {
            if (server.isSingleplayerOwner(player.getGameProfile())) {
                continue;
            }

            PacketDistributor.sendToPlayer(player, message);
        }
    }
}