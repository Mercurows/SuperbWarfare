package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum VehicleFireMessage {

    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                var player = context.getSender();

                if (player.getVehicle() instanceof VehicleEntity vehicle) {
                    vehicle.vehicleShoot(player, vehicle.getSeatIndex(player));
                }
            }
        });
        context.setPacketHandled(true);
    }

}
