package com.atsuishio.superbwarfare.network.message;

import com.atsuishio.superbwarfare.ModUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientIndicatorMessage(int messageType, int value) implements CustomPacketPayload {
    public static final Type<ClientIndicatorMessage> TYPE = new Type<>(ModUtils.loc("client_indicator"));

    public static final StreamCodec<ByteBuf, ClientIndicatorMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ClientIndicatorMessage::messageType,
            ByteBufCodecs.INT,
            ClientIndicatorMessage::value,
            ClientIndicatorMessage::new
    );

    public static void handler(final ClientIndicatorMessage message, final IPayloadContext context) {
        // TODO indicator process
//        ClientPacketHandler.handleClientIndicatorMessage(message, ctx);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
