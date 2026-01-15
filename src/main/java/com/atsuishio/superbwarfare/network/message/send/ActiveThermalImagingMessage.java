package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ActiveThermalImagingMessage(boolean active) {

    public static void encode(ActiveThermalImagingMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
    }

    public static ActiveThermalImagingMessage decode(FriendlyByteBuf buffer) {
        return new ActiveThermalImagingMessage(buffer.readBoolean());
    }

    public static void handler(ActiveThermalImagingMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            PlayerVariable.modify(player, capability -> capability.activeThermalImaging = message.active);
        });
        context.setPacketHandled(true);
    }
}
