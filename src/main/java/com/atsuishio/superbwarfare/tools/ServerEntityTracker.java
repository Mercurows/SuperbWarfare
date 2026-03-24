package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

public class ServerEntityTracker {
    public static void tick() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<Entity> vehicleEntities = SeekTool.getVehicle(player.level());
            for (Entity entity : vehicleEntities) {
                if (entity != null) {
                    EntitySyncMessage packet = new EntitySyncMessage(
                        entity.getId(),
                        entity.level().dimension().location(),
                        entity.getX(), entity.getY(), entity.getZ()
                    );
                    MinecraftUtil.sendPacketTo(player, packet);
                }
            }
        }
    }
}