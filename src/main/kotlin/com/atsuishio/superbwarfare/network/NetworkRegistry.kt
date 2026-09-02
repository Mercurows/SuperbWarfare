package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import com.atsuishio.superbwarfare.tools.camelToSnake
import com.atsuishio.superbwarfare.tools.createStreamCodec
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadHandler
import net.neoforged.neoforge.network.registration.PayloadRegistrar

val payloadTypeMap = mutableMapOf<Class<*>, CustomPacketPayload.Type<*>>()

inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

internal inline fun <reified T : PacketPayload> playTo(reg: (CustomPacketPayload.Type<T>, StreamCodec<in RegistryFriendlyByteBuf, T>, IPayloadHandler<T>) -> Unit) {

    val codec = createStreamCodec<T>()
    val className = T::class.java.simpleName.substringBeforeLast("Message")

    val name = className.camelToSnake()

    val type = CustomPacketPayload.Type<T>(loc(name))
    payloadTypeMap[T::class.java] = type

    reg(type, codec) { msg, context -> with(msg) { context.handler() } }
}

internal inline fun <reified T : ServerPacketPayload> playToServer() {
    playTo<T> { type, codec, handler ->
        registrar!!.playToServer<T>(type, codec, handler)
    }
}

internal inline fun <reified T : ClientPacketPayload> playToClient() {
    playTo<T> { type, codec, handler ->
        registrar!!.playToClient<T>(type, codec, handler)
    }
}

internal var registrar: PayloadRegistrar? = null

fun initializeNetwork(event: RegisterPayloadHandlersEvent) {
    registrar = event.registrar("1")
    registerPayloads()
}

private fun registerPayloads() {
    registerGeneratedPayloads()
}
