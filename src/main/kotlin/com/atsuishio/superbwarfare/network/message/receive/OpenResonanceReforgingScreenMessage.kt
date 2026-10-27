package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.ResonanceReforgingScreen
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.mc
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos

@RegisterPacket
@Serializable
data class OpenResonanceReforgingScreenMessage(val tablePos: Long) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        mc.setScreen(ResonanceReforgingScreen(BlockPos.of(tablePos)))
    }
}
