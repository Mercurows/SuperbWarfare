package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientMouseHandler {
    public static Vec2 posO = new Vec2(0, 0);
    public static Vec2 posN = new Vec2(0, 0);
    public static Vec2 mousePos = new Vec2(0, 0);
    public static double PosX = 0;
    public static double lerpPosX = 0;
    public static double PosY = 0;
    public static double lerpPosY = 0;

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
    public static void handleClientTick(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        posO = posN;
        posN = MouseMovementHandler.getMousePos();

        if (!notInGame()) {
            mousePos = posN.add(posO.scale(-1));

            if (mousePos.x != 0) {
                lerpPosX = Mth.lerp(0.1, PosX, mousePos.x);
            }
            if (mousePos.y != 0) {
                lerpPosY = Mth.lerp(0.1, PosY, mousePos.y);
            }
        }

        lerpPosX = Mth.clamp(Mth.lerp(event.getPartialTick(), lerpPosX, 0), -1, 1);
        lerpPosY = Mth.clamp(Mth.lerp(event.getPartialTick(), lerpPosY, 0), -1, 1);


        if (isFreeCam(player)) {
            freeCameraYaw = Mth.clamp(freeCameraYaw + 4 * lerpPosX, -100, 100);
            freeCameraPitch = Mth.clamp(freeCameraPitch + 4 * lerpPosY, -90, 90);
        }

        float yaw = event.getYaw();
        float pitch = event.getPitch();

        event.setYaw((float) (yaw + freeCameraYaw));
        event.setPitch((float) (pitch + freeCameraPitch));

        if (!isFreeCam(player)) {
            freeCameraYaw *= 0.8;
            freeCameraPitch *= 0.8;
        }
    }
}
