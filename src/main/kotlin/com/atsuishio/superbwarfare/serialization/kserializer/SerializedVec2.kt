package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.world.phys.Vec2

typealias SerializedVec2 = @Serializable(Vec2Serializer::class) Vec2

object Vec2Serializer : KSerializer<Vec2> {
    override val descriptor = buildClassSerialDescriptor("Vec2") {
        element<Float>("x")
        element<Float>("y")
    }

    override fun serialize(encoder: Encoder, value: Vec2) {
        encoder.encodeSerializableValue(FloatArraySerializer(), floatArrayOf(value.x, value.y))
    }

    override fun deserialize(decoder: Decoder): Vec2 {
        val (x, y) = decoder.decodeSerializableValue(FloatArraySerializer())
        return Vec2(x, y)
    }
}