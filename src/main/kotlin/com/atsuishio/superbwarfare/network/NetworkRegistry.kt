package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.network.message.receive.CapabilitySyncMessage
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
    registerByteBufPayloads()
    registerGeneratedPayloads()
}

/**
 * 手写 ByteBuf 编解码的包无法被 `@RegisterPacket` 发现，需要在这里显式注册。
 *
 * 这些包与 KSP 生成的包共用同一个 messageID 计数器，所以注册顺序必须在客户端与服务端
 * 完全一致——两端运行同一份构建即可保证这一点。
 */
private fun registerByteBufPayloads() {
    val encoder = BiConsumer<CapabilitySyncMessage, FriendlyByteBuf> { value, buf -> value.writeTo(buf) }
    val decoder = Function<FriendlyByteBuf, CapabilitySyncMessage> { buf -> CapabilitySyncMessage.readFrom(buf) }
    val handler = BiConsumer<CapabilitySyncMessage, PayloadContext> { msg, context -> msg.handleInternal(msg, context) }

    NetworkRegistry.playToClient(CapabilitySyncMessage::class.java, encoder, decoder, handler)
}
