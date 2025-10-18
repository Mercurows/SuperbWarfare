package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.inventory.menu.ChargingStationMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShowChargingRangeMessage {
    private final boolean operation;

    public ShowChargingRangeMessage(boolean operation) {
        this.operation = operation;
    }

    public static ShowChargingRangeMessage decode(FriendlyByteBuf buffer) {
        return new ShowChargingRangeMessage(buffer.readBoolean());
    }

    public static void encode(ShowChargingRangeMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.operation);
    }

    public static void handler(ShowChargingRangeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;

            var player = context.getSender();
            var menu = player.containerMenu;
            if (menu instanceof ChargingStationMenu chargingStationMenu) {
                if (!chargingStationMenu.stillValid(player)) return;

                chargingStationMenu.setShowRange(message.operation);
            }
        });
        context.setPacketHandled(true);
    }
}
