package com.atsuishio.superbwarfare.network.message;

import com.atsuishio.superbwarfare.ModUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BreathMessage(boolean messageType) implements CustomPacketPayload {
    public static final Type<BreathMessage> TYPE = new Type<>(ModUtils.loc("breath"));

    public static final StreamCodec<ByteBuf, BreathMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            BreathMessage::messageType,
            BreathMessage::new
    );


    public static void handler(final BreathMessage message, final IPayloadContext context) {
        // TODO handler
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}