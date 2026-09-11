package com.atsuishio.superbwarfare.data.attachment

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = AttachmentModifierValueSerializer::class)
sealed interface AttachmentModifierValue {
    data class Constant(val value: Double) : AttachmentModifierValue

    data class Reference(
        val ref: String,
        val scale: Double = 1.0,
        val offset: Double = 0.0,
    ) : AttachmentModifierValue
}

object AttachmentModifierValueSerializer : KSerializer<AttachmentModifierValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("AttachmentModifierValue")

    override fun serialize(encoder: Encoder, value: AttachmentModifierValue) {
        require(encoder is JsonEncoder)

        val element: JsonElement = when (value) {
            is AttachmentModifierValue.Constant -> JsonPrimitive(value.value)
            is AttachmentModifierValue.Reference -> buildJsonObject {
                put("Ref", value.ref)
                put("Scale", value.scale)
                put("Offset", value.offset)
            }
        }

        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): AttachmentModifierValue {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()

        val primitive = element as? JsonPrimitive
        if (primitive != null) {
            return AttachmentModifierValue.Constant(
                primitive.doubleOrNull
                    ?: throw SerializationException("Attachment modifier constant must be a number")
            )
        }

        val obj = element as? JsonObject
            ?: throw SerializationException("Attachment modifier value must be a number or an object")
        val ref = obj["Ref"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: throw SerializationException("Attachment modifier reference requires a non-empty Ref")

        return AttachmentModifierValue.Reference(
            ref = ref,
            scale = obj.optionalDouble("Scale", 1.0),
            offset = obj.optionalDouble("Offset", 0.0),
        )
    }

    private fun JsonObject.optionalDouble(key: String, default: Double): Double {
        val element = this[key] ?: return default
        return element.jsonPrimitive.doubleOrNull
            ?: throw SerializationException("Attachment modifier $key must be a number")
    }
}
