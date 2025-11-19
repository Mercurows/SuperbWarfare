package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.UUID;

public record VehicleFireMessage(Optional<UUID> uuid, Optional<Vector3f> targetPos) implements CustomPacketPayload {
    public static final Type<VehicleFireMessage> TYPE = new Type<>(Mod.loc("vehicle_fire"));

    public static final StreamCodec<ByteBuf, VehicleFireMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            VehicleFireMessage::uuid,
            ByteBufCodecs.optional(ByteBufCodecs.VECTOR3F),
            VehicleFireMessage::targetPos,
            VehicleFireMessage::new
    );

    public static void handler(VehicleFireMessage message, final IPayloadContext context) {
        var player = context.player();

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            if (message.targetPos.isPresent()) {
                vehicle.vehicleShoot(player, message.uuid.orElse(null), new Vec3(message.targetPos.get()));
            } else {
                vehicle.vehicleShoot(player, message.uuid.orElse(null), null);
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
