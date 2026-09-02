package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

/**
 * 紧急夺回操控权：飞行员双击前进键(W)关闭自动盘旋。
 * 由客户端在检测到 0.5s 内双击前进键时发送。
 */
@RegisterPacket
object LoiterOverrideMessage : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle as? VehicleEntity ?: return
        if (vehicle.computed().engineType != EngineType.AIRCRAFT) return
        if (!vehicle.loiterActive) return

        vehicle.loiterActive = false
    }
}
