package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.ModColor.ModColorAdapter.Companion.COLOR_PATTERN
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import java.io.IOException
import java.util.regex.Pattern

// RGB Color
@Serializable(ModColorSerializer::class)
class ModColor {
    var color = -0x1

    constructor()

    constructor(color: Int) {
        this.color = -0x1000000 or color
    }

    fun get(): Int {
        return -0x1000000 or this.color
    }

    internal class ModColorAdapter : TypeAdapter<ModColor>() {
        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: ModColor) {
            out.value(value.color.toLong())
        }

        @Throws(IOException::class)
        override fun read(`in`: JsonReader): ModColor {
            val p = `in`.peek()
            val obj = ModColor()

            if (p == JsonToken.STRING) {
                val str = `in`.nextString().trim { it <= ' ' }.lowercase()
                val matcher = COLOR_PATTERN.matcher(str)

                if (matcher.matches()) {
                    val colorStr = matcher.group("color")
                    obj.color = -0x1000000 or colorStr.substring(colorStr.length - 6).toInt(16)
                } else {
                    Mod.LOGGER.warn("invalid color string: {}", str)
                }
            } else if (p == JsonToken.NUMBER) {
                obj.color = -0x1000000 or `in`.nextInt()
            } else if (p == JsonToken.NULL) {
                `in`.nextNull()
            } else {
                throw IllegalStateException("invalid color token $p")
            }

            return obj
        }

        companion object {
            val COLOR_PATTERN: Pattern = Pattern.compile("^(#|0x)?(?<color>[A-Fa-f0-9]{6,})$")
        }
    }
}

object ModColorSerializer : KSerializer<ModColor> {
    override val descriptor = buildClassSerialDescriptor("ModColor") {
        element<Int>("color")
    }

    override fun serialize(encoder: Encoder, value: ModColor) {
        encoder.encodeInt(value.get())
    }

    override fun deserialize(decoder: Decoder): ModColor {
        require(decoder is JsonDecoder) { "Only JsonDecoder is supported!" }

        val element = decoder.decodeJsonElement()
        if (element is JsonNull) return ModColor()

        require(element is JsonPrimitive) { "JsonPrimitive is required!" }

        if (element.isString) {
            val input = element.content
            val str = input.trim { it <= ' ' }.lowercase()
            val matcher = COLOR_PATTERN.matcher(str)

            if (matcher.matches()) {
                val colorStr = matcher.group("color")
                return ModColor(-0x1000000 or colorStr.substring(colorStr.length - 6).toInt(16))
            }
        } else {
            return ModColor(-0x1000000 or element.int)
        }

        throw SerializationException("Invalid color format: $element")
    }
}