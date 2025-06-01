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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMouseHandler {

    public static Vec2 posO = new Vec2(0, 0);
    public static Vec2 posN = new Vec2(0, 0);
    public static Vec2 mousePos = new Vec2(0, 0);
    public static double PosX = 0;
    public static double lerpPosX = 0;
    public static double PosY = 0;
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

        if (!notInGame()) {
            if (player.getVehicle() instanceof VehicleEntity vehicle) {

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

                if (Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
                    com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.sendToServer(new MouseMoveMessage(
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpPosX + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpPosY * i,
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpPosY + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpPosX * (vehicle.getRoll() < 0 ? -1 : 1))
                    );
                } else {
                    com.atsuishio.superbwarfare.Mod.PACKET_HANDLER.sendToServer(new MouseMoveMessage(lerpPosX, lerpPosY));
                }
            }
        }

//        lerpPosX = Mth.clamp(Mth.lerp(event.getPartialTick(), lerpPosX, 0), -1, 1);
//        lerpPosY = Mth.clamp(Mth.lerp(event.getPartialTick(), lerpPosY, 0), -1, 1);
//
//
//        if (isFreeCam(player)) {
//            freeCameraYaw = Mth.clamp(freeCameraYaw + 4 * lerpPosX, -100, 100);
//            freeCameraPitch = Mth.clamp(freeCameraPitch + 4 * lerpPosY, -90, 90);
//        }
//
//        float yaw = event.getYaw();
//        float pitch = event.getPitch();
//
//        event.setYaw((float) (yaw + freeCameraYaw));
//        event.setPitch((float) (pitch + freeCameraPitch));
//
//        if (!isFreeCam(player)) {
//            freeCameraYaw *= 0.8;
//            freeCameraPitch *= 0.8;
//        }
    }
}
