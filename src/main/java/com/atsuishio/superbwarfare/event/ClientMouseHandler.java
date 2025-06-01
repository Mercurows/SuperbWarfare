package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import com.atsuishio.superbwarfare.config.client.VehicleControlConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.AirEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.droneFovLerp;
import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
    public static void handleClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

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
                    PacketDistributor.sendToServer(new MouseMoveMessage(
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpPosX + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpPosY * i,
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpPosY + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpPosX * (vehicle.getRoll() < 0 ? -1 : 1))
                    );
                } else {
                    PacketDistributor.sendToServer(new MouseMoveMessage(lerpPosX, lerpPosY));
                }
            } else {
                PacketDistributor.sendToServer(new MouseMoveMessage(0, 0));
            }
        }
    }

    @SubscribeEvent
    public static void handleClientTick(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);

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

    @SubscribeEvent
    public static void calculatePlayerTurn(CalculatePlayerTurnEvent event) {
        var raw = event.getMouseSensitivity() * 0.6 + 0.2;
        var newSensitivity = changeSensitivity(raw) * invertY();
        event.setMouseSensitivity((newSensitivity - 0.2) / 0.6);
    }

    public static float invertY() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 反转鼠标

        if (player == null) return 1;

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle instanceof AirEntity && vehicle.getFirstPassenger() == player) {
            return VehicleControlConfig.INVERT_AIRCRAFT_CONTROL.get() ? -1 : 1;
        }
        return 1;
    }

    private static double changeSensitivity(double original) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return original;

        if (player.hasEffect(ModMobEffects.SHOCK) && !player.isSpectator()) {
            return 0;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            float customSens = data.sensitivity.get();

            if (!player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                return original / Math.max((1 + (0.2 * (data.zoom() - (0.3 * customSens)) * ClientEventHandler.zoomTime)), 0.1);
            }
        }

        if (stack.is(ModItems.MONITOR.get()) && NBTTool.getTag(stack).getBoolean("Using") && NBTTool.getTag(stack).getBoolean("Linked")) {
            return 0.33 / (1 + 0.08 * (droneFovLerp - 1));
        }

        if (isFreeCam(player)) {
            return 0;
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            return vehicle.getSensitivity(original, ClientEventHandler.zoomVehicle, vehicle.getSeatIndex(player), vehicle.onGround());
        }

        return original;
    }
}
