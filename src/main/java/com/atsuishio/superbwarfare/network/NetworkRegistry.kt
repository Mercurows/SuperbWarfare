package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.handling.IPayloadHandler

val payloadTypeMap = mutableMapOf<Class<*>, CustomPacketPayload.Type<*>>()

private inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

private inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

private inline fun <reified T : PacketPayload<T>> playTo(reg: (CustomPacketPayload.Type<T>, StreamCodec<in RegistryFriendlyByteBuf, T>, IPayloadHandler<T>) -> Unit) {

    val codec: StreamCodec<FriendlyByteBuf, T> = StreamCodec.of(
        { buf, value -> encodeTo(buf, value) },
        { buf -> decodeFrom(buf) },
    )

    val className = T::class.java.simpleName.substringBefore("Message")

    val name = buildString {
        append(className[0].lowercase())

        for (i in 1 until className.length) {
            val c = className[i]
            if (c.isUpperCase()) {
                append("_")
            }
            append(className[i].lowercase())
        }
    }

    val type = CustomPacketPayload.Type<T>(loc(name))
    payloadTypeMap[T::class.java] = type

    reg(type, codec) { msg, context -> msg.handler(msg, context) }
}

private inline fun <reified T : PacketPayload<T>> playToServer() {
    playTo<T> { type, codec, handler -> NetworkRegistry.playToServer(type, codec, handler) }
}

private inline fun <reified T : PacketPayload<T>> playToClient() {
    playTo<T> { type, codec, handler -> NetworkRegistry.playToClient(type, codec, handler) }
}

fun register() {
    playToClient<ClientIndicatorMessage>()
}