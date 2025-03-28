package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record BreathMessage(boolean messageType) implements CustomPacketPayload {
    public static final Type<BreathMessage> TYPE = new Type<>(Mod.loc("breath"));

    public static final StreamCodec<ByteBuf, BreathMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            BreathMessage::messageType,
            BreathMessage::new
    );


    public static void handler(final BreathMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap == null) return;
        if (message.messageType
                && !cap.breathExhaustion
                && cap.zoom
                && player.getPersistentData().getDouble("NoBreath") == 0
        ) {
            cap.breath = true;
            cap.syncPlayerVariables(player);
        }

        if (!message.messageType) {
            cap.breath = false;
            cap.syncPlayerVariables(player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}