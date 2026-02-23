package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.CameraType

object ResetCameraTypeMessage : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        options.cameraType = ClientEventHandler.lastCameraType ?: CameraType.FIRST_PERSON
    }
}
