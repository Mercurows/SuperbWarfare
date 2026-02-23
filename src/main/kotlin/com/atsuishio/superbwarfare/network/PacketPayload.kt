package com.atsuishio.superbwarfare.network

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext

typealias PayloadContext = IPayloadContext

sealed class PacketPayload : CustomPacketPayload {
    override fun type() = payloadTypeMap[this::class.java]!!
    abstract fun PayloadContext.handler()
}

abstract class ServerPacketPayload : PacketPayload() {
    fun PayloadContext.sender() = player() as ServerPlayer
}

abstract class ClientPacketPayload : PacketPayload()