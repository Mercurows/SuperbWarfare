package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum VehicleFireMessage implements CustomPacketPayload {
    INSTANCE;
    public static final Type<VehicleFireMessage> TYPE = new Type<>(Mod.loc("vehicle_fire"));

    public static final StreamCodec<ByteBuf, VehicleFireMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler(final IPayloadContext context) {
        var player = context.player();

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            vehicle.vehicleShoot(player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
