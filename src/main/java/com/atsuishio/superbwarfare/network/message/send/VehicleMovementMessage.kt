package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import kotlinx.serialization.Serializable

@Serializable
data class VehicleMovementMessage(val keys: Short) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val entity = player.vehicle
        val stack = player.mainHandItem
        val tag = stack.getOrCreateTag()

        val vehicle = if (entity is VehicleEntity && entity.getFirstPassenger() === player) {
            entity
        } else if (stack.`is`(ModItems.MONITOR.get())
            && tag.getBoolean("Using")
            && tag.getBoolean("Linked")
        ) {
            EntityFindUtil.findDrone(player.level(), tag.getString("LinkedDrone")) ?: return
        } else return

        vehicle.processInput(keys)
    }
}
