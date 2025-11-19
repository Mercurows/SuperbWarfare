package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum StopVehicleSeekSoundMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            if (player.getVehicle() instanceof VehicleEntity vehicle) {
                var gunData = vehicle.getGunData(player);
                if (gunData != null) {
                    var location = gunData.compute().soundInfo.locking.getLocation();
                    player.connection.send(new ClientboundStopSoundPacket(location, SoundSource.PLAYERS));
                }
            }

        });
        context.setPacketHandled(true);
    }
}
