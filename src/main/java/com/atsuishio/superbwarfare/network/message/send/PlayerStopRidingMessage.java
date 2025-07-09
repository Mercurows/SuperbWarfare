package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum PlayerStopRidingMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            var vehicle = player.getVehicle();
            if (!(vehicle instanceof VehicleEntity)) return;

            player.stopRiding();
            player.setJumping(false);
        });
        ctx.get().setPacketHandled(true);
    }
}
