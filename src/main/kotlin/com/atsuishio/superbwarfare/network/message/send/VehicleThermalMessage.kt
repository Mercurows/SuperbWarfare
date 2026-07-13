package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/**
 * PJM: keep-alive от клиента, пока включён тепловизор техники. Пока пакеты идут, сервер держит
 * счётчик и расходует энергию быстрее (см. VehicleEntity.thermalDrainCooldown). Когда клиент
 * перестаёт слать (ТПВ выключен / вышел из техники / отключился), счётчик обнуляется сам.
 */
@Serializable
data class VehicleThermalMessage(val keepAlive: Boolean = true) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return

        // Сервер сам проверяет, что игрок реально в тепловизорном сиденье.
        val seat = vehicle.computed().seats().getOrNull(vehicle.getSeatIndex(player)) ?: return
        if (!seat.hasThermalImaging) return

        vehicle.thermalDrainCooldown = 6
    }
}
