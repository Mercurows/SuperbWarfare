package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DrawClientMessage {

    public boolean draw;

    public DrawClientMessage(boolean draw) {
        this.draw = draw;
    }

    public static void encode(DrawClientMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.draw);
    }

    public static DrawClientMessage decode(FriendlyByteBuf buffer) {
        return new DrawClientMessage(buffer.readBoolean());
    }

    public static void handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClientEventHandler::resetGunStatus));
        context.get().setPacketHandled(true);
    }
}
