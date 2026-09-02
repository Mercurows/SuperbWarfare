package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

/**
 * 卸载乘客消息。
 * - unloadAll = true：主驾驶双击卸载乘客键时发送，强制让除主驾驶以外的所有乘客离开载具。
 * - unloadAll = false：主驾驶按住卸载乘客键时每隔1秒发送，让序号最靠后的一位乘客（非主驾驶）离开载具。
 */
@Serializable
@RegisterPacket
data class VehicleUnloadPassengersMessage(val unloadAll: Boolean = false) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return

        // 仅主驾驶可以卸载乘客
        if (vehicle.firstPassenger != player) return

        if (unloadAll) {
            // 收集除主驾驶以外的所有乘客并让其下车
            val passengers = vehicle.passengers.toList()
            for (passenger in passengers) {
                if (passenger != player) {
                    passenger.stopRiding()
                }
            }
        } else {
            // 找到序号最靠后的乘客（非主驾驶）并让其下车
            val passengers = vehicle.passengers.toList()
            val lastPassenger = passengers.lastOrNull { it != player }
            lastPassenger?.stopRiding()
        }
    }
}
