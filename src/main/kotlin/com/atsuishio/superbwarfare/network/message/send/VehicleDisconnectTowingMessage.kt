package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

/**
 * 主驾驶双击断开牵引键时发送，断开载具的牵引关系。
 * 同时处理"牵引别人"和"被别人牵引"两种情况。
 * 由客户端在检测到 0.5s 内双击断开牵引键时发送。
 */
@RegisterPacket
object VehicleDisconnectTowingMessage : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return

        // 仅主驾驶可以断开牵引
        if (vehicle.firstPassenger != player) return

        vehicle.clearTowingInfo()
    }
}
