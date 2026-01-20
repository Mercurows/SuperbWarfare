package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.network.message.receive.ClientSetMotionMessage
import com.atsuishio.superbwarfare.network.message.receive.DataSyncMessage
import com.atsuishio.superbwarfare.network.message.send.*
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

private inline fun <reified T : PacketPayload> playTo(reg: (CustomPacketPayload.Type<T>, StreamCodec<in RegistryFriendlyByteBuf, T>, IPayloadHandler<T>) -> Unit) {

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

    reg(type, codec) { msg, context -> with(msg) { context.handler() } }
}

private inline fun <reified T : ServerPacketPayload> playToServer() {
    playTo<T> { type, codec, handler -> NetworkRegistry.playToServer(type, codec, handler) }
}

private inline fun <reified T : ClientPacketPayload> playToClient() {
    playTo<T> { type, codec, handler -> NetworkRegistry.playToClient(type, codec, handler) }
}

fun register() {
    playToClient<ClientIndicatorMessage>()
    playToClient<ClientSetMotionMessage>()
    playToClient<DataSyncMessage>()

    playToServer<AdjustMortarAngleMessage>()
    playToServer<AdjustZoomFovMessage>()
    playToServer<AimVillagerMessage>()
    playToServer<AssembleVehicleMessage>()
    playToServer<ChangeVehicleSeatMessage>()
}