package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShootClientMessage {

    public double time;

    public ShootClientMessage(double time) {
        this.time = time;
    }

    public static void encode(ShootClientMessage message, FriendlyByteBuf buffer) {
        buffer.writeDouble(message.time);
    }

    public static ShootClientMessage decode(FriendlyByteBuf buffer) {
        return new ShootClientMessage(buffer.readDouble());
    }

    public static void handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClientEventHandler::handleClientShoot));
        context.get().setPacketHandled(true);
    }
}
