package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.joml.Vector3f

typealias SerializedVector3f = @Serializable(Vector3fSerializer::class) Vector3f

object Vector3fSerializer : KSerializer<Vector3f> {
    override val descriptor = buildClassSerialDescriptor("Vector3f") {
        element<Float>("x")
        element<Float>("y")
        element<Float>("z")
    }

    override fun serialize(encoder: Encoder, value: Vector3f) {
        encoder.encodeSerializableValue(FloatArraySerializer(), floatArrayOf(value.x, value.y, value.z))
    }

    override fun deserialize(decoder: Decoder): Vector3f {
        val (x, y, z) = decoder.decodeSerializableValue(FloatArraySerializer())
        return Vector3f(x, y, z)
    }
}