package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.menu.DogTagEditorMenu;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ClientPacketHandler {

    public static void handleResetCameraType() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        Minecraft.getInstance().options.setCameraType(Objects.requireNonNullElse(ClientEventHandler.lastCameraType, CameraType.FIRST_PERSON));
    }

    public static void handleDogTagEditorMessage(int containerId, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu.containerId == containerId) {
            ((DogTagEditorMenu) mc.player.containerMenu).stack = stack;
            if (mc.screen instanceof DogTagEditorScreen dogTagEditorScreen) {
                dogTagEditorScreen.stack = stack;
            }
        }
    }
}
