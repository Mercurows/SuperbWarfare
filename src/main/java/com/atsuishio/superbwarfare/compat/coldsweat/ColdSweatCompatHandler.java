package com.atsuishio.superbwarfare.compat.coldsweat;

import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArmedVehicleEntity;
import com.momosoftworks.coldsweat.api.util.Temperature;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class ColdSweatCompatHandler {

    public static void onPlayerInVehicle(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        if (player.getVehicle() instanceof ArmedVehicleEntity vehicle && vehicle.hidePassenger(player)) {
            Temperature.set(player, Temperature.Trait.WORLD, 1);
        }
    }

    public static boolean hasMod() {
        return ModList.get().isLoaded(CompatHolder.COLD_SWEAT);
    }
}
