package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMouseHandler {

    public static Vec2 posO = new Vec2(0, 0);
    public static Vec2 posN = new Vec2(0, 0);
    public static double lerpPosX = 0;
    public static double lerpPosY = 0;

    public static double speedX = 0;
    public static double speedY = 0;


    public static double freeCameraPitch = 0;
    public static double freeCameraYaw = 0;

    private static boolean notInGame() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        if (mc.getOverlay() != null) return true;
        if (mc.screen != null) return true;
        if (!mc.mouseHandler.isMouseGrabbed()) return true;
        return !mc.isWindowActive();
    }

    @SubscribeEvent
    public static void handleClientTick(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;


        if (player == null) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        posO = posN;
        posN = MouseMovementHandler.getMousePos();

        if (!notInGame() && player.getVehicle() instanceof VehicleEntity vehicle) {
            speedX = 0.1 * (posN.x - posO.x);
            speedY = 0.1 * (posN.y - posO.y);

            lerpPosX = Mth.lerp(0.4, lerpPosX, speedX);
            lerpPosY = Mth.lerp(0.4, lerpPosY, speedY);

            double i = 0;

            if (vehicle.getRoll() < 0) {
                i = 1;
            } else if (vehicle.getRoll() > 0) {
                i = -1;
            }

            if (Mth.abs(vehicle.getRoll()) > 90) {
                i *= (1 - (Mth.abs(vehicle.getRoll()) - 90) / 90);
            }

            if (!isFreeCam(player)) {
                if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                    com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.sendToServer(new MouseMoveMessage(
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpPosX + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpPosY * i,
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpPosY + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpPosX * (vehicle.getRoll() < 0 ? -1 : 1))
                    );
                } else {
                    com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.sendToServer(new MouseMoveMessage(lerpPosX, lerpPosY));
                }
            } else {
                com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.sendToServer(new MouseMoveMessage(0, 0));
            }
        }
    }

    @SubscribeEvent
    public static void handleClientTick(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float times = Minecraft.getInstance().getDeltaFrameTime();

        if (isFreeCam(player)) {
            freeCameraYaw -= 0.4f * times * lerpPosX;
            freeCameraPitch += 0.2f * times * lerpPosY;
        } else {
            freeCameraYaw = Mth.lerp(0.075 * event.getPartialTick(), freeCameraYaw, 0);
            freeCameraPitch = Mth.lerp(0.075 * event.getPartialTick(), freeCameraPitch, 0);
        }

        while (freeCameraYaw > 180F) {
            freeCameraYaw -= 360;
        }
        while (freeCameraYaw <= -180F) {
            freeCameraYaw += 360;
        }
        while (freeCameraPitch > 180F) {
            freeCameraPitch -= 360;
        }
        while (freeCameraPitch <= -180F) {
            freeCameraPitch += 360;
        }
    }
}
