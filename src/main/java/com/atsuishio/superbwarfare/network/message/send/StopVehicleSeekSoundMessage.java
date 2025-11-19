package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum StopVehicleSeekSoundMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<StopVehicleSeekSoundMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("stop_vehicle_seek_sound"));

    public static final StreamCodec<ByteBuf, StopVehicleSeekSoundMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler(final IPayloadContext context) {
        var player = (ServerPlayer) context.player();

        if (player.getVehicle() instanceof VehicleEntity vehicle) {
            var gunData = vehicle.getGunData(player);
            if (gunData != null) {
                var location = gunData.compute().soundInfo.locking.getLocation();
                player.connection.send(new ClientboundStopSoundPacket(location, SoundSource.PLAYERS));
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
