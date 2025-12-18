package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum DrawClientMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEventHandler::resetGunStatus));
        context.get().setPacketHandled(true);
    }
}
