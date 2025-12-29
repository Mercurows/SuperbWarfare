package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import java.util.function.BiConsumer
import java.util.function.Function

private inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

private inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

private inline fun <reified T : PacketPayload<T>> playTo(
    dist: Dist,
    reg: (BiConsumer<T, FriendlyByteBuf>, Function<FriendlyByteBuf, T>, BiConsumer<T, PayloadContext>) -> Unit
) {
    reg(
        { value, buf -> encodeTo(buf, value) },
        { buf -> decodeFrom(buf) },
        { msg, context -> msg.handleInternal(msg, context, dist) }
    )
}

private inline fun <reified T : PacketPayload<T>> playToServer() {
    playTo<T>(Dist.DEDICATED_SERVER) { enc, dec, handler ->
        NetworkRegistry.playToServer(T::class.java, enc, dec, handler)
    }
}

private inline fun <reified T : PacketPayload<T>> playToClient() {
    playTo<T>(Dist.CLIENT) { enc, dec, handler ->
        NetworkRegistry.playToClient(T::class.java, enc, dec, handler)
    }
}

fun register() {
    playToClient<ClientIndicatorMessage>()
}