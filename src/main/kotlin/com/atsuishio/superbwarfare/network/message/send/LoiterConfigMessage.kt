package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.command.resolveSafeY
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import org.joml.Quaternionf
import kotlin.math.max
import kotlin.math.min

@Serializable
data class LoiterConfigMessage(
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val radius: Float,
    val active: Boolean,
    val skipTerrain: Boolean
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return
        if (vehicle.computed().engineType != EngineType.AIRCRAFT) return

        // Clamp radius to valid range [200, 10000]
        val clampedRadius = min(10000f, max(200f, radius))

        // Apply terrain safety check on Y (reuse resolveSafeY from LoiterCommand)
        val finalY = if (skipTerrain) {
            centerY
        } else {
            resolveSafeY(vehicle, centerX.toInt(), centerY.toInt(), centerZ.toInt())
        }

        vehicle.loiterParams = Quaternionf(centerX, finalY, centerZ, clampedRadius)
        vehicle.loiterActive = active
    }
}
