package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ResetCameraTypeMessage(int msgType) implements CustomPacketPayload {
    public static final Type<ResetCameraTypeMessage> TYPE = new Type<>(Mod.loc("reset_camera_type"));

    public static final StreamCodec<ByteBuf, ResetCameraTypeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ResetCameraTypeMessage::msgType,
            ResetCameraTypeMessage::new
    );

    public static void handler(ResetCameraTypeMessage message, final IPayloadContext context) {
        ClientPacketHandler.handleResetCameraType();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
