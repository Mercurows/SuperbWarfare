package com.atsuishio.superbwarfare.serialization.kserializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.minecraft.resources.ResourceLocation

typealias SerializedResourceLocation = @Serializable(ResourceLocationSerializer::class) ResourceLocation

object ResourceLocationSerializer : KSerializer<ResourceLocation> {
    override val descriptor = PrimitiveSerialDescriptor("ResourceLocation", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ResourceLocation) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ResourceLocation {
        return ResourceLocation.tryParse(decoder.decodeString())!!
    }
}