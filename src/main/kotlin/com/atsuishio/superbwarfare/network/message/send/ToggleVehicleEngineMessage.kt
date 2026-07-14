package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.Component

@Serializable
data object ToggleVehicleEngineMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return
        if (vehicle.firstPassenger !== player || !vehicle.hasManualEngineControl()) return

        if (!vehicle.engineOn && vehicle.hasEnergyStorage() && vehicle.energy <= 0) {
            player.displayClientMessage(Component.translatable("tips.superbwarfare.vehicle_engine.no_energy"), true)
            return
        }

        if (vehicle.engineOn) {
            vehicle.engineOn = false
            vehicle.engineStartupTicksRemaining = 0
            vehicle.stopEngineMotion()
        } else {
            vehicle.engineOn = true
            vehicle.engineStartupTicksRemaining = vehicle.engineStartupDurationTicks()
        }

        player.displayClientMessage(
            Component.translatable(
                when {
                    !vehicle.engineOn -> "tips.superbwarfare.vehicle_engine.stopped"
                    vehicle.engineStarting() -> "tips.superbwarfare.vehicle_engine.starting"
                    else -> "tips.superbwarfare.vehicle_engine.started"
                }
            ),
            true
        )
    }
}
