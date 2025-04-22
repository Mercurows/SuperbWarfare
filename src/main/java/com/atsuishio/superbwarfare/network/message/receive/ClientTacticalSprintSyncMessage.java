package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ClientTacticalSprintSyncMessage(boolean flag) implements CustomPacketPayload {
    public static final Type<ClientTacticalSprintSyncMessage> TYPE = new Type<>(Mod.loc("client_tactical_sprint_sync"));

    public static final StreamCodec<ByteBuf, ClientTacticalSprintSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientTacticalSprintSyncMessage::flag,
            ClientTacticalSprintSyncMessage::new
    );

    public static void handler(ClientTacticalSprintSyncMessage message) {
        MiscConfig.ALLOW_TACTICAL_SPRINT.set(message.flag);
        MiscConfig.ALLOW_TACTICAL_SPRINT.save();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
