package com.atsuishio.superbwarfare.network

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadContext

typealias PayloadContext = IPayloadContext

abstract class PacketPayload : CustomPacketPayload {
    override fun type() = payloadTypeMap[this::class.java]!!
    abstract fun PayloadContext.handler()
}