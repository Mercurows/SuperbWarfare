package com.atsuishio.superbwarfare.compat.coldsweat;

import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;

public class ColdSweatCompatHandler {

    public static void onPlayerInVehicle(TickEvent.PlayerTickEvent event) {
        var player = event.player;
        if (player == null) return;
        if (player.getVehicle() instanceof VehicleEntity vehicle
                && vehicle.hasEnergyStorage()
                && vehicle.isEnclosed(vehicle.getSeatIndex(player))
                && vehicle.getEnergy() > 0
        ) {
            Temperature.set(player, Temperature.Trait.CORE, 1);
        }
    }

    public static boolean hasMod() {
        return ModList.get().isLoaded(CompatHolder.COLD_SWEAT);
    }
}
