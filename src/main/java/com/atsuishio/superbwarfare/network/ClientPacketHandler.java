package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.network.message.receive.FinishAssemblingVehicleMessage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public class ClientPacketHandler {

    public static void handleResetCameraType() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        Minecraft.getInstance().options.setCameraType(Objects.requireNonNullElse(ClientEventHandler.lastCameraType, CameraType.FIRST_PERSON));
    }

    public static void handleFinishAssemblingVehicleMessage(FinishAssemblingVehicleMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;
        if (player.containerMenu.containerId != message.containerId()) return;
        if (minecraft.screen instanceof VehicleAssemblingScreen screen) {
            screen.finishAssembling();
        }
    }
}
