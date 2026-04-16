package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.serialization.kserializer.Vec3Serializer
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.world.phys.Vec3
import java.io.IOException

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

    internal class StringOrVec3Adapter : TypeAdapter<StringOrVec3?>() {
        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: StringOrVec3?) {
            if (value == null) {
                out.nullValue()
                return
            }

            if (value.string != null) {
                out.value(value.string)
            } else {
                out.beginArray()
                checkNotNull(value.vec3)
                out.value(value.vec3.x)
                out.value(value.vec3.y)
                out.value(value.vec3.z)
                out.endArray()
            }
        }

        @Throws(IOException::class)
        override fun read(`in`: JsonReader): StringOrVec3 {
            if (`in`.peek() == JsonToken.NULL) {
                Mod.LOGGER.warn("null StringOrVec3 value!")
                `in`.nextNull()
                return StringOrVec3()
            }

            if (`in`.peek() == JsonToken.STRING) {
                return StringOrVec3(`in`.nextString())
            }

            if (`in`.peek() == JsonToken.BEGIN_ARRAY) {
                `in`.beginArray()
                val x = `in`.nextDouble()
                val y = `in`.nextDouble()
                val z = `in`.nextDouble()
                `in`.endArray()
                return StringOrVec3(Vec3(x, y, z))
            }

            throw IllegalStateException("invalid StringOrVec3 value!")
        }
    }
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

        val element = decoder.decodeJsonElement()

        return if (element is JsonPrimitive) {
            StringOrVec3(element.jsonPrimitive.content)
        } else {
            StringOrVec3(decoder.decodeSerializableValue(Vec3Serializer))
        }
    }
}
