package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.inventory.menu.VehicleAssemblingMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AssembleVehicleMessage {

    private final ResourceLocation id;
    private final int containerId;

    public AssembleVehicleMessage(ResourceLocation id, int containerId) {
        this.id = id;
        this.containerId = containerId;
    }

    public static void encode(AssembleVehicleMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeResourceLocation(message.id);
        byteBuf.writeVarInt(message.containerId);
    }

    public static AssembleVehicleMessage decode(FriendlyByteBuf byteBuf) {
        return new AssembleVehicleMessage(byteBuf.readResourceLocation(), byteBuf.readVarInt());
    }

    public static void handler(AssembleVehicleMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu.containerId != message.containerId) return;
            if (player.containerMenu instanceof VehicleAssemblingMenu menu) {
                menu.assembleVehicle(message.id, player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
