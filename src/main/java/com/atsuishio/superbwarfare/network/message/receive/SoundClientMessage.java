package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.UUID;

public record SoundClientMessage(
        ResourceLocation location,
        Vector3f pos,
        float radius,
        float pitch,
        UUID uuid
) implements CustomPacketPayload {

    public static final Type<SoundClientMessage> TYPE = new Type<>(Mod.loc("sound_client"));

    public static final StreamCodec<ByteBuf, SoundClientMessage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            SoundClientMessage::location,
            ByteBufCodecs.VECTOR3F,
            SoundClientMessage::pos,
            ByteBufCodecs.FLOAT,
            SoundClientMessage::radius,
            ByteBufCodecs.FLOAT,
            SoundClientMessage::pitch,
            UUIDUtil.STREAM_CODEC,
            SoundClientMessage::uuid,
            SoundClientMessage::new
    );

    public static void handler(SoundClientMessage message) {
        ClientPacketHandler.handleSoundClientMessage(message);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
