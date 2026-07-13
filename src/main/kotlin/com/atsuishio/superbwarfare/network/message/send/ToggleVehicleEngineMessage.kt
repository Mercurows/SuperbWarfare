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

        vehicle.engineOn = !vehicle.engineOn
        if (!vehicle.engineOn) {
            vehicle.stopEngineMotion()
        }

        player.displayClientMessage(
            Component.translatable(
                if (vehicle.engineOn) "tips.superbwarfare.vehicle_engine.started"
                else "tips.superbwarfare.vehicle_engine.stopped"
            ),
            true
        )
    }
}
