package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.network.message.receive.ClientSetMotionMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PlayerStopRidingMessage {

    private final boolean ejection;

    public PlayerStopRidingMessage(boolean ejection) {
        this.ejection = ejection;
    }

    public static PlayerStopRidingMessage decode(FriendlyByteBuf buffer) {
        return new PlayerStopRidingMessage(buffer.readBoolean());
    }

    public static void encode(PlayerStopRidingMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.ejection);
    }

    public static void handler(PlayerStopRidingMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            var vehicle = player.getVehicle();
            if (vehicle instanceof VehicleEntity vehicle1) {
                if (message.ejection) {
                    var vec = vehicle1.getDismountMovement(player, vehicle1.getTagSeatIndex(player));
                    Mod.queueServerWork(1, () -> Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientSetMotionMessage(vec)));
                }
                player.stopRiding();
                player.setJumping(false);
            }
        });
        context.setPacketHandled(true);
    }
}
