package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MouseMoveMessage {

    private final double speedX;
    private final double speedY;

    public MouseMoveMessage(double speedX, double speedY) {
        this.speedX = speedX;
        this.speedY = speedY;
    }


    public static void encode(MouseMoveMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeDouble(message.speedX);
        byteBuf.writeDouble(message.speedY);
    }

    public static MouseMoveMessage decode(FriendlyByteBuf byteBuf) {
        return new MouseMoveMessage(byteBuf.readDouble(), byteBuf.readDouble());
    }

    public static void handler(MouseMoveMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            var entity = player.getVehicle();

            if (entity instanceof VehicleEntity vehicle) {
                vehicle.mouseInput(message.speedX, message.speedY);
            }
        });
        context.get().setPacketHandled(true);
    }
}
