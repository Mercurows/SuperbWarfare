package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.event.DeployedWeaponHandler
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload

@RegisterPacket
object DeployWeaponMessage : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        if (player.isSpectator) return

        if (DeployedWeaponHandler.isDeployed(player)) {
            DeployedWeaponHandler.undeploy(player)
        } else {
            DeployedWeaponHandler.deploy(player)
        }
    }
}
