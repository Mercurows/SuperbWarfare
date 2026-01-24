package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable
import net.minecraft.util.Mth

@Serializable
data class SwitchVehicleWeaponMessage(
    val index: Int,
    val value: Double,
    val isScroll: Boolean,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val vehicle = player.vehicle as? VehicleEntity ?: return
        if (vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            val value = if (isScroll) {
                (if (value > 0) Mth.ceil(value) else Mth.floor(value)).coerceIn(-1, 1).toDouble()
            } else value

            vehicle.changeWeapon(index, value.toInt(), isScroll)
        }
    }
}
