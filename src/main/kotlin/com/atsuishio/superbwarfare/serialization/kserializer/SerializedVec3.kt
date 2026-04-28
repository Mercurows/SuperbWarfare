package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.DoubleArraySerializer
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
        encoder.encodeSerializableValue(DoubleArraySerializer(), doubleArrayOf(value.x, value.y, value.z))
    }

    override fun deserialize(decoder: Decoder): Vec3 {
        val (x, y, z) = decoder.decodeSerializableValue(DoubleArraySerializer())
        return Vec3(x, y, z)
    }
}