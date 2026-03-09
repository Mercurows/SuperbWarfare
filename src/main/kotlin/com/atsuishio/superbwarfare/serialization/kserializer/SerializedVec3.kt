package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.world.phys.Vec3

typealias SerializedVec3 = @Serializable(Vec3Serializer::class) Vec3

object Vec3Serializer : KSerializer<Vec3> {
    override val descriptor = buildClassSerialDescriptor("Vec3") {
        element<Double>("x")
        element<Double>("y")
        element<Double>("z")
    }

    override fun serialize(encoder: Encoder, value: Vec3) {
        encoder.encodeDouble(value.x)
        encoder.encodeDouble(value.y)
        encoder.encodeDouble(value.z)
    }

    override fun deserialize(decoder: Decoder): Vec3 {
        return Vec3(decoder.decodeDouble(), decoder.decodeDouble(), decoder.decodeDouble())
    }
}