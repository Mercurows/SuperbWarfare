package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PlayerStopRidingMessage(int msgType) implements CustomPacketPayload {
    public static final Type<PlayerStopRidingMessage> TYPE = new Type<>(Mod.loc("player_stop_riding"));

    public static final StreamCodec<ByteBuf, PlayerStopRidingMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PlayerStopRidingMessage::msgType,
            PlayerStopRidingMessage::new
    );

    public static void handler(PlayerStopRidingMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        var vehicle = player.getVehicle();
        if (!(vehicle instanceof VehicleEntity)) return;

        player.stopRiding();
        player.setJumping(false);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
