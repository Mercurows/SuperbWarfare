package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public enum DrawClientMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<DrawClientMessage> TYPE = new Type<>(Mod.loc("draw_client"));

    public static final StreamCodec<ByteBuf, DrawClientMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler() {
        ClientEventHandler.resetGunStatus();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
