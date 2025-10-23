package com.atsuishio.superbwarfare.data.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.network.message.receive.VehiclesDataMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
public class VehicleDataTool {

    public static HashMap<String, DefaultVehicleData> vehicleData = CustomData.VEHICLE_DATA;

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
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        var players = event.getPlayerList();
        var server = players.getServer();

        var message = VehiclesDataMessage.create();
        for (var player : players.getPlayers()) {
            if (server.isSingleplayerOwner(player.getGameProfile())) {
                continue;
            }

            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }
}