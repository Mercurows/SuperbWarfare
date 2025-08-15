package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper;
import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen;
import com.atsuishio.superbwarfare.config.client.KillMessageConfig;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.KillMessageHandler;
import com.atsuishio.superbwarfare.menu.EnergyMenu;
import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.tools.PlayerKillRecord;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ClientPacketHandler {

    public static void handlePlayerKillMessage(Player attacker, Entity target, boolean headshot, ResourceKey<DamageType> damageType, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            if (KillMessageHandler.QUEUE.size() >= KillMessageConfig.KILL_MESSAGE_COUNT.get()) {
                KillMessageHandler.QUEUE.poll();
            }
            KillMessageHandler.QUEUE.offer(new PlayerKillRecord(attacker, target, attacker.getMainHandItem(), headshot, damageType));
        }
    }

    public static void handleClientIndicatorMessage(ClientIndicatorMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            switch (message.type) {
                case 1 -> CrossHairOverlay.HEAD_INDICATOR = message.value;
                case 2 -> CrossHairOverlay.KILL_INDICATOR = message.value;
                case 3 -> CrossHairOverlay.VEHICLE_INDICATOR = message.value;
                default -> CrossHairOverlay.HIT_INDICATOR = message.value;
            }
        }
    }

    public static void handleContainerDataMessage(int containerId, List<ContainerDataMessage.Pair> data, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu.containerId == containerId) {
                data.forEach(p -> ((EnergyMenu) mc.player.containerMenu).setData(p.id, p.data));
            }
        }
    }

    public static void handleRadarMenuOpen(RadarMenuOpenMessage message, Supplier<NetworkEvent.Context> ctx) {
        FuMO25ScreenHelper.resetEntities();
        FuMO25ScreenHelper.pos = message.pos;
    }

    public static void handleRadarMenuClose() {
        FuMO25ScreenHelper.resetEntities();
        FuMO25ScreenHelper.pos = null;
    }

    public static void handleResetCameraType(Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;

            Minecraft.getInstance().options.setCameraType(Objects.requireNonNullElse(ClientEventHandler.lastCameraType, CameraType.FIRST_PERSON));
        }
    }

    public static void handleClientSyncMotion(ClientMotionSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity entity = level.getEntity(message.id);
            if (entity != null) {
                entity.lerpMotion(message.x, message.y, message.z);
            }
        }
    }

    public static void handleClientTacticalSprintSync(boolean flag, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            MiscConfig.ALLOW_TACTICAL_SPRINT.set(flag);
            MiscConfig.ALLOW_TACTICAL_SPRINT.save();
        }
    }

    public static void handleClientSetMotion(ClientSetMotionMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player != null) {
                player.setDeltaMovement(message.motion().x, message.motion().y, message.motion().z);
            }
        }
    }

    public static void handleFinishAssemblingVehicleMessage(FinishAssemblingVehicleMessage message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player == null) return;
            if (player.containerMenu.containerId != message.containerId()) return;
            if (minecraft.screen instanceof VehicleAssemblingScreen screen) {
                screen.finishAssembling();
            }
        }
    }
}
