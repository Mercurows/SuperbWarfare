package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.client.MouseMovementHandler;
import com.atsuishio.superbwarfare.config.client.VehicleControlConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AirEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
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

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isFreeCam;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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

        if (notInGame()) {
            speedX = 0;
            speedY = 0;
            lerpSpeedX = 0;
            lerpSpeedY = 0;
        }

        posO = posN;
        posN = MouseMovementHandler.getMousePos();

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);

        if (stack.is(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            DroneEntity drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"));
            if (drone != null) {
                if (notInGame()) {
                    PacketDistributor.sendToServer(new MouseMoveMessage(0, 0));
                    return;
                }
                speedX = drone.getMouseSensitivity() * (posN.x - posO.x);
                speedY = drone.getMouseSensitivity() * (posN.y - posO.y);

                lerpSpeedX = Mth.lerp(drone.getMouseSpeedX(), lerpSpeedX, speedX);
                lerpSpeedY = Mth.lerp(drone.getMouseSpeedY(), lerpSpeedY, speedY);

                PacketDistributor.sendToServer(new MouseMoveMessage(lerpSpeedX, lerpSpeedY));
            }
            return;
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && player == vehicle.getFirstPassenger()) {
            if (notInGame()) {
                PacketDistributor.sendToServer(new MouseMoveMessage(0, 0));
                return;
            }

            int y = 1;

            if (vehicle instanceof AirEntity && VehicleControlConfig.INVERT_AIRCRAFT_CONTROL.get()) {
                y = -1;
            }

            speedX = vehicle.getMouseSensitivity() * (posN.x - posO.x);
            speedY = y * vehicle.getMouseSensitivity() * (posN.y - posO.y);

            lerpSpeedX = Mth.lerp(vehicle.getMouseSpeedX(), lerpSpeedX, speedX);
            lerpSpeedY = Mth.lerp(vehicle.getMouseSpeedY(), lerpSpeedY, speedY);

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
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedX + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedY * i,
                            (1 - (Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedY + ((Mth.abs(vehicle.getRoll()) / 90)) * lerpSpeedX * (vehicle.getRoll() < 0 ? -1 : 1))
                    );
                } else {
                    PacketDistributor.sendToServer(new MouseMoveMessage(lerpSpeedX, lerpSpeedY));
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

        if (notInGame()) {
            freeCameraYaw = 0;
            freeCameraPitch = 0;
            return;
        }

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);

        freeCameraYaw -= 0.4f * times * lerpSpeedX;
        freeCameraPitch += 0.3f * times * lerpSpeedY;
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
            return 0;
        }

        if (isFreeCam(player)) {
            return 0;
        }

        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.banHand(player)) {
            return vehicle.getSensitivity(original, ClientEventHandler.zoomVehicle, vehicle.getSeatIndex(player), vehicle.onGround());
        }

        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);
            float customSens = data.sensitivity.get();

            if (!player.getMainHandItem().isEmpty() && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                return original / Math.max((1 + (0.2 * (data.zoom() - (0.3 * customSens)) * ClientEventHandler.zoomTime)), 0.1);
            }
        }

        return original;
    }
}
