package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.MiscConfig;
import com.atsuishio.superbwarfare.init.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TacticalSprintMessage(boolean sprint) implements CustomPacketPayload {
    public static final Type<TacticalSprintMessage> TYPE = new Type<>(Mod.loc("tactical_sprint"));

    public static final StreamCodec<ByteBuf, TacticalSprintMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            TacticalSprintMessage::sprint,
            TacticalSprintMessage::new
    );

    public static void handler(TacticalSprintMessage message, final IPayloadContext context) {
        var player = context.player();

        var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
        cap.tacticalSprint = MiscConfig.ALLOW_TACTICAL_SPRINT.get() && message.sprint;
        cap.sync(player);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
