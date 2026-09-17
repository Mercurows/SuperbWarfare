package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.serialization.kserializer.Vec3Serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.minecraft.world.phys.Vec3

@Serializable(StringOrVec3Serializer::class)
class StringOrVec3 {
    val string: String?
    val vec3: Vec3?

    constructor(string: String?) {
        this.string = string
        this.vec3 = null
    }

    @JvmOverloads
    constructor(vec3: Vec3? = Vec3.ZERO) {
        this.vec3 = vec3
        this.string = null
    }

    val isString get() = string != null
    val isVec3 get() = vec3 != null

}

object StringOrVec3Serializer : KSerializer<StringOrVec3> {
    override val descriptor = buildClassSerialDescriptor("StringOrVec3") {
        element<String?>("string")
        element<SerializedVec3?>("vec3")
    }

    override fun serialize(encoder: Encoder, value: StringOrVec3) {
        if (value.string != null) {
            encoder.encodeString(value.string)
        } else {
            encoder.encodeSerializableValue(Vec3Serializer, value.vec3!!)
        }
    }

    override fun deserialize(decoder: Decoder): StringOrVec3 {
        require(decoder is JsonDecoder) { "only JsonDecoder is supported!" }

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> StringOrVec3(element.content)
            is JsonArray -> {
                if (element.size < 3) {
                    throw SerializationException("Expected a 3-element array for StringOrVec3, but had $element")
                }
                StringOrVec3(
                    Vec3(
                        element[0].jsonPrimitive.double,
                        element[1].jsonPrimitive.double,
                        element[2].jsonPrimitive.double
                    )
                )
            }

            else -> throw SerializationException("Expected a string or a 3-element array for StringOrVec3, but had $element")
        }
    }
}
