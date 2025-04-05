package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ShootClientMessage(double time) implements CustomPacketPayload {
    public static final Type<ShootClientMessage> TYPE = new Type<>(Mod.loc("shoot_client"));

    public static final StreamCodec<ByteBuf, ShootClientMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShootClientMessage::time,
            ShootClientMessage::new
    );

    public static void handler(ShootClientMessage message, final IPayloadContext context) {
        ClientEventHandler.handleClientShoot();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
