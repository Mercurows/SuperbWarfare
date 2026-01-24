package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.GunEventHandler.tryStartReload
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

object ReloadMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val vehicle = player.vehicle

        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            vehicle.modifyGunData(vehicle.getSeatIndex(player)) { data -> tryStartReload(vehicle.ammoSupplier, data) }
            return
        }

        val stack = player.mainHandItem
        if (stack.item !is GunItem) return
        tryStartReload(player, from(stack))
    }
}
