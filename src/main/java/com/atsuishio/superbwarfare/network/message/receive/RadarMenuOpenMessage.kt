package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.screens.FuMO25ScreenHelper
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos

@Serializable
data class RadarMenuOpenMessage(@Contextual var pos: BlockPos) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        FuMO25ScreenHelper.resetEntities()
        FuMO25ScreenHelper.pos = pos
    }
}
