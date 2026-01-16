package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import com.atsuishio.superbwarfare.config.client.ControlConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;
import static com.atsuishio.superbwarfare.event.ClientEventHandler.zoomVehicle;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMouseHandler {

    public static Vec2 posO = new Vec2(0, 0);
    public static Vec2 posN = new Vec2(0, 0);
    public static double lerpSpeedX = 0;
    public static double lerpSpeedY = 0;

    public static double speedX = 0;
    public static double speedY = 0;

    public static double freeCameraPitch = 0;
    public static double freeCameraYaw = 0;

    public static double custom3pDistance = 0;
    public static double custom3pDistanceLerp = 0;

    public static double mouseXMoveTick = 0;
    public static double mouseYMoveTick = 0;

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

        if (notInGame()) {
            speedX = 0;
            speedY = 0;
            lerpSpeedX = 0;
            lerpSpeedY = 0;
        }

        posO = posN;
        posN = MouseMovementHandler.getMousePos();

        ItemStack stack = player.getMainHandItem();

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone"));
            if (drone != null) {
                if (notInGame()) {
                    NetworkRegistry.PACKET_HANDLER.sendToServer(new MouseMoveMessage(0, 0));
                    return;
                }
                speedX = (drone.getMouseSensitivity() / ClientEventHandler.droneFovLerp) * (posN.x - posO.x);
                speedY = (drone.getMouseSensitivity() / ClientEventHandler.droneFovLerp) * (posN.y - posO.y);

                lerpSpeedX = Mth.lerp(0.3, lerpSpeedX, speedX);
                lerpSpeedY = Mth.lerp(0.3, lerpSpeedY, speedY);

                NetworkRegistry.PACKET_HANDLER.sendToServer(new MouseMoveMessage(lerpSpeedX, lerpSpeedY));
            }
            return;
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && player == vehicle.getFirstPassenger() && (vehicle.getVehicleType() == VehicleType.AIRPLANE || vehicle.getVehicleType() == VehicleType.HELICOPTER)) {
            if (notInGame()) {
                NetworkRegistry.PACKET_HANDLER.sendToServer(new MouseMoveMessage(0, 0));
                return;
            }

            int y = 1;

            if (ControlConfig.INVERT_AIRCRAFT_CONTROL.get()) {
                y = -1;
            }

            speedX = vehicle.getMouseSensitivity() * (posN.x - posO.x) * (zoomVehicle ? 0.3 : 1);
            speedY = y * vehicle.getMouseSensitivity() * (posN.y - posO.y) * (zoomVehicle ? 0.4 : 1);

            mouseXMoveTick = Mth.lerp(0.1, mouseXMoveTick, speedX);
            mouseYMoveTick = Mth.lerp(0.1, mouseYMoveTick, speedY);

            if (vehicle.getVehicleType() == VehicleType.AIRPLANE) {
                lerpSpeedX = Mth.lerp(Math.max(0.006 * Mth.abs((float) mouseXMoveTick), 0.12), lerpSpeedX, speedX);
                lerpSpeedY = Mth.lerp(Math.max(0.005 * Mth.abs((float) mouseYMoveTick), 0.12), lerpSpeedY, speedY);
            } else {
                lerpSpeedX = Mth.lerp(Math.max(0.0045 * Mth.abs((float) mouseXMoveTick), 0.1), lerpSpeedX, speedX * 0.5);
                lerpSpeedY = Mth.lerp(Math.max(0.0035 * Mth.abs((float) mouseYMoveTick), 0.1), lerpSpeedY, speedY * 0.5);
            }

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
                    if (!(vehicle instanceof Tom6Entity)) {
                        NetworkRegistry.PACKET_HANDLER.sendToServer(new MouseMoveMessage(
                                (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedX + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedY * i,
                                (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedY + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedX * (vehicle.getRoll() < 0 ? -1 : 1))
                        );
                    }
                } else {
                    NetworkRegistry.PACKET_HANDLER.sendToServer(new MouseMoveMessage(lerpSpeedX, lerpSpeedY));
                }
            } else {
                NetworkRegistry.PACKET_HANDLER.sendToServer(new MouseMoveMessage(0, 0));
            }
        }
    }

    @SubscribeEvent
    public static void handleClientTick(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (notInGame()) {
            freeCameraYaw = 0;
            freeCameraPitch = 0;
            return;
        }

        float times = Minecraft.getInstance().getDeltaFrameTime();

        freeCameraYaw -= 0.2f * times * lerpSpeedX;
        freeCameraPitch += 0.15f * times * lerpSpeedY;
        if (!isFreeCam(player)) {
            freeCameraYaw = Mth.lerp(0.6 * times, freeCameraYaw, 0);
            freeCameraPitch = Mth.lerp(0.6 * times, freeCameraPitch, 0);
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

        custom3pDistanceLerp = Mth.lerp(times, custom3pDistanceLerp, custom3pDistance);
    }

    public static int invertY() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 反转鼠标
        if (player == null) return 1;

        if (player.getVehicle() instanceof VehicleEntity vehicle
                && (vehicle.getVehicleType() == VehicleType.AIRPLANE || vehicle.getVehicleType() == VehicleType.HELICOPTER)
                && vehicle.getFirstPassenger() == player) {
            return ControlConfig.INVERT_AIRCRAFT_CONTROL.get() ? -1 : 1;
        }
        return 1;
    }

    public static double changeSensitivity(double original) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return original;

        if (player.hasEffect(ModMobEffects.SHOCK.get()) && !player.isSpectator()) {
            return 0;
        }

        ItemStack stack = mc.player.getMainHandItem();

        if (stack.is(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using") && stack.getOrCreateTag().getBoolean("Linked")) {
            return 0;
        }

        if (isFreeCam(player)) {
            return 0;
        }

        if (player.isUsingItem() && player.getUseItem().is(ModItems.ARTILLERY_INDICATOR.get()) && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return original / Math.max(1 + 0.2 * ClientEventHandler.artilleryIndicatorZoom, 0.1);
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.banHand(player)) {
            return vehicle.getSensitivity(original, ClientEventHandler.zoomVehicle, vehicle.getSeatIndex(player), vehicle.onGround());
        }

        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            float customSens = data.sensitivity.get();

            if (!player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                return original / Math.max((1 + (0.2 * (data.zoom() - (0.3 * customSens)) * ClientEventHandler.zoomTime)), 0.1) * (ControlConfig.MOUSE_SENSITIVITY.get() / 100f);
            }
        }

        return original;
    }
}
