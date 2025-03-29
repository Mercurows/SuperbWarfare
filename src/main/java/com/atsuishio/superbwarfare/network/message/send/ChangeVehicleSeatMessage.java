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

public record ChangeVehicleSeatMessage(int index) implements CustomPacketPayload {
    public static final Type<ChangeVehicleSeatMessage> TYPE = new Type<>(Mod.loc("change_vehicle_seat"));

    public static final StreamCodec<ByteBuf, ChangeVehicleSeatMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ChangeVehicleSeatMessage::index,
            ChangeVehicleSeatMessage::new
    );

    public static void handler(ChangeVehicleSeatMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (!(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }

        vehicle.changeSeat(player, message.index);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
