package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientPhosphorusFireMessage {
    public int id;
    public boolean flag;

    public ClientPhosphorusFireMessage(int id, boolean flag) {
        this.id = id;
        this.flag = flag;
    }

    public static void encode(ClientPhosphorusFireMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.id);
        buffer.writeBoolean(message.flag);
    }

    public static ClientPhosphorusFireMessage decode(FriendlyByteBuf buffer) {
        return new ClientPhosphorusFireMessage(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handler(ClientPhosphorusFireMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handlePhosphorusFire(message, ctx)));
        ctx.get().setPacketHandled(true);
    }
}
