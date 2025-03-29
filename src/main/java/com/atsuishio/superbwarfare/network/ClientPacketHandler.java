package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class ClientPacketHandler {

    //    public static void handleSimulationDistanceMessage(int distance, final IPayloadContext context) {
//        if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
//            DroneUIOverlay.MAX_DISTANCE = distance * 16;
//        }
//    }
//
    public static void handleResetCameraType() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        Minecraft.getInstance().options.setCameraType(Objects.requireNonNullElse(ClientEventHandler.lastCameraType, CameraType.FIRST_PERSON));
    }
}
