package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import kotlinx.serialization.Serializable

@Serializable
data class MouseMoveMessage(val speedX: Double, val speedY: Double) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val entity = player.vehicle

        if (entity is VehicleEntity) {
            entity.mouseInput(speedX, speedY)
        }

        val stack = player.mainHandItem
        val tag = stack.getOrCreateTag()

        if (stack.`is`(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")) {
            val drone = EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone"))
            if (drone != null) {
                drone.mouseInput(speedX, speedY)
            }
        }
    }
}
