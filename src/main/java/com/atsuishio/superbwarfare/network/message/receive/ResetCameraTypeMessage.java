package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public enum ResetCameraTypeMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<ResetCameraTypeMessage> TYPE = new Type<>(Mod.loc("reset_camera_type"));

    public static final StreamCodec<ByteBuf, ResetCameraTypeMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler() {
        ClientPacketHandler.handleResetCameraType();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
