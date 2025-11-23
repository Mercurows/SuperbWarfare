package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.network.message.receive.FinishAssemblingVehicleMessage;
import com.atsuishio.superbwarfare.network.message.receive.SoundClientMessage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

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

    public static void handleSoundClientMessage(SoundClientMessage message) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        if (player.getUUID().equals(message.uuid())
                && (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON || zoomVehicle)
        ) return;

        var sound = SoundEvent.createVariableRangeEvent(message.location());

        double distance = player.position().distanceTo(new Vec3(message.pos()));
        int time = (int) (distance / 17);

        if (time == 0) {
            player.level().playSound(player, message.pos().x(), message.pos().y(), message.pos().z(), sound, SoundSource.BLOCKS, message.radius(), message.pitch());
        } else {
            Mod.queueClientWork(time,
                    () -> player.level().playSound(player, message.pos().x(), message.pos().y(), message.pos().z(), sound, SoundSource.BLOCKS, message.radius(), message.pitch()));
        }
    }

}
