package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FinishAssemblingVehicleMessage(int containerId) {

    public static void encode(FinishAssemblingVehicleMessage message, FriendlyByteBuf byteBuf) {
        byteBuf.writeVarInt(message.containerId);
    }

    public static FinishAssemblingVehicleMessage decode(FriendlyByteBuf byteBuf) {
        return new FinishAssemblingVehicleMessage(byteBuf.readVarInt());
    }

    public static void handler(FinishAssemblingVehicleMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleFinishAssemblingVehicleMessage(message, ctx)));
        ctx.get().setPacketHandled(true);
    }
}
