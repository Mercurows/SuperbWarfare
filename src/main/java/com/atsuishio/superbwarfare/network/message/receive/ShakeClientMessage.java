package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public record ShakeClientMessage(
        double time, double radius, double amplitude,
        double x, double y, double z
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShakeClientMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("shake_client"));

    public static final StreamCodec<ByteBuf, ShakeClientMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::time,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::radius,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::amplitude,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::x,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::y,
            ByteBufCodecs.DOUBLE,
            ShakeClientMessage::z,
            ShakeClientMessage::new
    );

    public static void handler(final ShakeClientMessage message, final IPayloadContext context) {
        ClientEventHandler.handleShakeClient(message.time, message.radius, message.amplitude, message.x, message.y, message.z, context);
    }


    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
