package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.ClientSetMotionMessage
import com.atsuishio.superbwarfare.network.message.send.*
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

private inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

private inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

private inline fun <reified T : PacketPayload> playTo(
    reg: (BiConsumer<T, FriendlyByteBuf>, Function<FriendlyByteBuf, T>, BiConsumer<T, PayloadContext>) -> Unit
) {
    reg(
        { value, buf -> encodeTo(buf, value) },
        { buf -> decodeFrom(buf) },
        { msg, context -> msg.handleInternal(msg, context) }
    )
}

private inline fun <reified T : ServerPacketPayload> playToServer() {
    playTo<T> { enc, dec, handler ->
        NetworkRegistry.playToServer(T::class.java, enc, dec, handler)
    }
}

private inline fun <reified T : ClientPacketPayload> playToClient() {
    playTo<T> { enc, dec, handler ->
        NetworkRegistry.playToClient(T::class.java, enc, dec, handler)
    }
}

fun register() {
    playToClient<ClientIndicatorMessage>()
    playToClient<ClientSetMotionMessage>()

    playToServer<AdjustMortarAngleMessage>()
    playToServer<AdjustZoomFovMessage>()
    playToServer<AimVillagerMessage>()
    playToServer<AssembleVehicleMessage>()
    playToServer<ChangeVehicleSeatMessage>()
}