package com.atsuishio.superbwarfare.compat.coldsweat

import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.momosoftworks.coldsweat.api.util.Temperature
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.tick.PlayerTickEvent

object ColdSweatCompatHandler {
    @JvmStatic
    fun onPlayerInVehicle(event: PlayerTickEvent.Pre) {
        val player = event.entity ?: return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity
            && vehicle.hasEnergyStorage()
            && vehicle.isEnclosed(vehicle.getSeatIndex(player))
            && vehicle.energy > 0
        ) {
            Temperature.set(player, Temperature.Trait.CORE, 1.0)
        }
    }

    fun hasMod(): Boolean {
        return ModList.get().isLoaded(CompatHolder.COLD_SWEAT)
    }
}
