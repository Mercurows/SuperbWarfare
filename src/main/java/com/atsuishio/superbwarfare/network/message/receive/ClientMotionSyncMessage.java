package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientMotionSyncMessage(int id, float x, float y, float z) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientMotionSyncMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("client_motion_sync"));

    public static final StreamCodec<ByteBuf, ClientMotionSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ClientMotionSyncMessage::id,
            ByteBufCodecs.FLOAT,
            ClientMotionSyncMessage::x,
            ByteBufCodecs.FLOAT,
            ClientMotionSyncMessage::y,
            ByteBufCodecs.FLOAT,
            ClientMotionSyncMessage::z,
            ClientMotionSyncMessage::new
    );

    public ClientMotionSyncMessage(Entity entity) {
        this(entity.getId(), (float) entity.getDeltaMovement().x, (float) entity.getDeltaMovement().y, (float) entity.getDeltaMovement().z);
    }


    public static void handler(final ClientMotionSyncMessage message, final IPayloadContext context) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        Entity entity = level.getEntity(message.id);
        if (entity != null) {
            entity.lerpMotion(message.x, message.y, message.z);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
