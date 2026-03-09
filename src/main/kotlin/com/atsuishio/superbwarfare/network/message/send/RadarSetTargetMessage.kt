package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import kotlinx.serialization.Serializable

@Serializable
data class RadarSetTargetMessage(val target: SerializedUUID) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()

        val menu = player.containerMenu as? FuMO25Menu ?: return
        if (!menu.stillValid(player)) return

        menu.selfPos.ifPresent {
            EntityFindUtil.getEntities(player.level()).getAll()
                .asSequence()
                .filterIsInstance<AutoAimableEntity>()
                .filter { it.getOwner() === player && it.distanceTo(player) <= 24 }
                .forEach { it.targetUUID = target.toString() }
        }
    }
}