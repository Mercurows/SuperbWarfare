package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record ClientSetMotionMessage(Vector3f motion) implements CustomPacketPayload {

    public static final Type<ClientSetMotionMessage> TYPE = new Type<>(Mod.loc("client_set_motion"));

    public static final StreamCodec<ByteBuf, ClientSetMotionMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VECTOR3F,
            ClientSetMotionMessage::motion,
            ClientSetMotionMessage::new
    );

    public static void handler(ClientSetMotionMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            player.setDeltaMovement(message.motion().x, message.motion().y, message.motion().z);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
