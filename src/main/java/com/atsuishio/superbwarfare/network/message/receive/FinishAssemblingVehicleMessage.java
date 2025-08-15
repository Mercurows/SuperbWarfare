package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.network.ClientPacketHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record FinishAssemblingVehicleMessage(int containerId) implements CustomPacketPayload {
    public static final Type<FinishAssemblingVehicleMessage> TYPE = new Type<>(Mod.loc("finish_assembling_vehicle"));

    public static final StreamCodec<ByteBuf, FinishAssemblingVehicleMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, FinishAssemblingVehicleMessage::containerId,
            FinishAssemblingVehicleMessage::new
    );

    public static void handler(FinishAssemblingVehicleMessage message) {
        ClientPacketHandler.handleFinishAssemblingVehicleMessage(message);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
