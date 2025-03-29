package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record VehicleFireMessage(int msgType) implements CustomPacketPayload {
    public static final Type<VehicleFireMessage> TYPE = new Type<>(Mod.loc("vehicle_fire"));

    public static final StreamCodec<ByteBuf, VehicleFireMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            VehicleFireMessage::msgType,
            VehicleFireMessage::new
    );

    public static void handler(VehicleFireMessage message, final IPayloadContext context) {
        var player = context.player();
        if (player.getVehicle() instanceof ArmedVehicleEntity iVehicle) {
            iVehicle.vehicleShoot(player, message.msgType);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
