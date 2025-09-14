package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TDMSyncMessage(TDMSavedData data) {

    public static void encode(TDMSyncMessage message, FriendlyByteBuf buf) {
        buf.writeCollection(message.data.getEntities(), FriendlyByteBuf::writeUtf);
    }

    public static TDMSyncMessage decode(FriendlyByteBuf buf) {
        return new TDMSyncMessage(new TDMSavedData(buf.readList(FriendlyByteBuf::readUtf)));
    }

    public static void handler(TDMSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleTDMSyncMessage(message, ctx)));
        ctx.get().setPacketHandled(true);
    }
}
