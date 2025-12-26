package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ClientPhosphorusFireMessage(int id, boolean flag) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientPhosphorusFireMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("client_phosphorus_fire"));

    public static final StreamCodec<ByteBuf, ClientPhosphorusFireMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ClientPhosphorusFireMessage::id,
            ByteBufCodecs.BOOL,
            ClientPhosphorusFireMessage::flag,
            ClientPhosphorusFireMessage::new
    );

    public static void handler(ClientPhosphorusFireMessage message) {
        ClientPacketHandler.handlePhosphorusFire(message);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
