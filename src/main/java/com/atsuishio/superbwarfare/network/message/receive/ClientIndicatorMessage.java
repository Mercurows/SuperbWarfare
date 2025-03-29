package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientIndicatorMessage(int messageType, int value) implements CustomPacketPayload {
    public static final Type<ClientIndicatorMessage> TYPE = new Type<>(Mod.loc("client_indicator"));

    public static final StreamCodec<ByteBuf, ClientIndicatorMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ClientIndicatorMessage::messageType,
            ByteBufCodecs.INT,
            ClientIndicatorMessage::value,
            ClientIndicatorMessage::new
    );

    public static void handler(final ClientIndicatorMessage message, final IPayloadContext context) {
        var type = message.messageType();
        switch (type) {
            case 1 -> CrossHairOverlay.HEAD_INDICATOR = message.value();
            case 2 -> CrossHairOverlay.KILL_INDICATOR = message.value();
            case 3 -> CrossHairOverlay.VEHICLE_INDICATOR = message.value();
            default -> CrossHairOverlay.HIT_INDICATOR = message.value();
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
