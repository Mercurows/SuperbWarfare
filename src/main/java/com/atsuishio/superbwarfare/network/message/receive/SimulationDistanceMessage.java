package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.DroneHudOverlay;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SimulationDistanceMessage(int distance) implements CustomPacketPayload {
    public static final Type<SimulationDistanceMessage> TYPE = new Type<>(Mod.loc("simulation_distance"));

    public static final StreamCodec<ByteBuf, SimulationDistanceMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SimulationDistanceMessage::distance,
            SimulationDistanceMessage::new
    );

    public static void handler(SimulationDistanceMessage message, final IPayloadContext context) {
        DroneHudOverlay.MAX_DISTANCE = message.distance * 16;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
