package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum VehicleFireMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                if (player.getVehicle() instanceof VehicleEntity vehicle) {
                    vehicle.vehicleShoot(player);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
