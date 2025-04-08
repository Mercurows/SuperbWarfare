package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BreathMessage(boolean msgType) implements CustomPacketPayload {
    public static final Type<BreathMessage> TYPE = new Type<>(Mod.loc("breath"));

    public static final StreamCodec<ByteBuf, BreathMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            BreathMessage::msgType,
            BreathMessage::new
    );


    public static void handler(final BreathMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
        if (message.msgType
                && !cap.breathExhaustion
                && cap.zoom
                && player.getPersistentData().getDouble("NoBreath") == 0
        ) {
            cap.breath = true;
            player.setData(ModAttachments.PLAYER_VARIABLE, cap);
            cap.sync(player);
        }

        if (!message.msgType) {
            cap.breath = false;
            player.setData(ModAttachments.PLAYER_VARIABLE, cap);
            cap.sync(player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}