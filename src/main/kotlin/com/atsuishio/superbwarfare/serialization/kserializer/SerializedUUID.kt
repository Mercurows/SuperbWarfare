package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

typealias SerializedUUID = @Serializable(UUIDSerializer::class) UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = buildClassSerialDescriptor("UUID") {
        element<Long>("mostSignificantBits")
        element<Long>("leastSignificantBits")
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeLong(value.mostSignificantBits)
        encoder.encodeLong(value.leastSignificantBits)
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID(decoder.decodeLong(), decoder.decodeLong())
    }
}