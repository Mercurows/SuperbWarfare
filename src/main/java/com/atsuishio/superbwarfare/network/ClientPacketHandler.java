package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandler {

    public static void handleClientIndicatorMessage(ClientIndicatorMessage message, final IPayloadContext context) {
        var type = message.messageType();
        switch (type) {
            case 1 -> CrossHairOverlay.HEAD_INDICATOR = message.value();
            case 2 -> CrossHairOverlay.KILL_INDICATOR = message.value();
            case 3 -> CrossHairOverlay.VEHICLE_INDICATOR = message.value();
            default -> CrossHairOverlay.HIT_INDICATOR = message.value();
        }
    }

//    public static void handleSimulationDistanceMessage(int distance, final IPayloadContext context) {
//        if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
//            DroneUIOverlay.MAX_DISTANCE = distance * 16;
//        }
//    }
//
//    public static void handleContainerDataMessage(int containerId, List<ContainerDataMessage.Pair> data, final IPayloadContext context) {
//        if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
//            Minecraft mc = Minecraft.getInstance();
//            if (mc.player != null && mc.player.containerMenu.containerId == containerId) {
//                data.forEach(p -> ((EnergyMenu) mc.player.containerMenu).setData(p.id, p.data));
//            }
//        }
//    }
//
//    public static void handleRadarMenuOpen(RadarMenuOpenMessage message, final IPayloadContext context) {
//        FuMO25ScreenHelper.resetEntities();
//        FuMO25ScreenHelper.pos = message.pos;
//    }
//
//    public static void handleRadarMenuClose() {
//        FuMO25ScreenHelper.resetEntities();
//        FuMO25ScreenHelper.pos = null;
//    }
//
//    public static void handleResetCameraType(final IPayloadContext context) {
//        if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
//            Minecraft minecraft = Minecraft.getInstance();
//            Player player = minecraft.player;
//            if (player == null) return;
//
//            Minecraft.getInstance().options.setCameraType(Objects.requireNonNullElse(ClientEventHandler.lastCameraType, CameraType.FIRST_PERSON));
//        }
//    }
//
//    public static void handleClientSyncMotion(ClientMotionSyncMessage message, final IPayloadContext context) {
//        if (context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
//            var level = Minecraft.getInstance().level;
//            if (level == null) return;
//            Entity entity = level.getEntity(message.id);
//            if (entity != null) {
//                entity.lerpMotion(message.x, message.y, message.z);
//            }
//        }
//    }
}
