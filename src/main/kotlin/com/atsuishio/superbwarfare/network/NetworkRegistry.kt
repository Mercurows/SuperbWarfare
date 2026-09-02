package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

internal inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

internal inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

internal inline fun <reified T : PacketPayload> playTo(
    reg: (BiConsumer<T, FriendlyByteBuf>, Function<FriendlyByteBuf, T>, BiConsumer<T, PayloadContext>) -> Unit
) {
    val instance = T::class.objectInstance
    if (instance != null) {
        reg({ _, _ -> }, { instance }, { msg, context -> msg.handleInternal(msg, context) })
    } else {
        reg(
            { value, buf -> encodeTo(buf, value) },
            { buf -> decodeFrom(buf) },
            { msg, context -> msg.handleInternal(msg, context) }
        )
    }
}

internal inline fun <reified T : ServerPacketPayload> playToServer() {
    playTo<T> { enc, dec, handler ->
        NetworkRegistry.playToServer(T::class.java, enc, dec, handler)
    }
}

internal inline fun <reified T : ClientPacketPayload> playToClient() {
    playTo<T> { enc, dec, handler ->
        NetworkRegistry.playToClient(T::class.java, enc, dec, handler)
    }
}

fun initializeNetwork() {
    registerGeneratedPayloads()
}
