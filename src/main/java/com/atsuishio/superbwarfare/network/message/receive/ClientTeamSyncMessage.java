package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientTeamSyncMessage(String name, boolean flag) {

    public static void encode(ClientTeamSyncMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.name);
        buffer.writeBoolean(message.flag);
    }

    public static ClientTeamSyncMessage decode(FriendlyByteBuf buffer) {
        return new ClientTeamSyncMessage(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handler(ClientTeamSyncMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleClientTeamSync(message, ctx)));
        ctx.get().setPacketHandled(true);
    }
}
