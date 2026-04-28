package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.io.IOException

@Serializable
class CollisionLevel {
    /**
     * 碰撞等级，范围是0~4
     * 0 - 无法撞坏方块
     * 1 - 允许撞坏软方块
     * 2 - 允许撞坏普通方块
     * 3 - 允许撞坏硬方块
     * 4 - 允许野兽撞击模式
     */
    @SerialName("Level")
    var level: Int = 2

    @SerialName("PowerLimits")
    var powerLimits: MutableList<Limit> = mutableListOf()

    @Serializable(LimitSerializer::class)
    @JvmRecord
    data class Limit(val power: Float, val motion: Float, val equals: Boolean) {
        override fun toString(): String {
            return "[$power, $motion, $equals]"
        }
    }

    object LimitSerializer : KSerializer<Limit> {
        override val descriptor = buildClassSerialDescriptor("CollisionLevelLimit") {
            element<Float>("power")
            element<Float>("motion")
            element<Boolean>("equals")
        }

        override fun serialize(
            encoder: Encoder,
            value: Limit
        ) {
            require(encoder is JsonEncoder) { "Only JsonEncoder is supported" }
            val jsonArray = JsonArray(
                listOf(
                    JsonPrimitive(value.power),
                    JsonPrimitive(value.motion),
                    JsonPrimitive(value.equals)
                )
            )
            encoder.encodeJsonElement(jsonArray)
        }

        override fun deserialize(decoder: Decoder): Limit {
            require(decoder is JsonDecoder) { "Only JsonDecoder is supported" }

            val element = decoder.decodeJsonElement()
            if (element is JsonNull) {
                throw SerializationException("Unexpected null for Limit")
            }

            require(element is JsonArray && element.size >= 3) {
                "Expected JSON array with at least 3 elements, got $element"
            }

            val power = element[0].jsonPrimitive.content.toFloatOrNull()
            val motion = element[1].jsonPrimitive.content.toFloatOrNull()
            val equals = element[2].jsonPrimitive.boolean

            require(power != null && motion != null) {
                "Invalid float values in array: $element"
            }

            return Limit(power, motion, equals)
        }
    }

    class LimitAdapter : TypeAdapter<Limit>() {
        @Throws(IOException::class)
        override fun write(out: JsonWriter, value: Limit?) {
            if (value == null) {
                out.nullValue()
                return
            }

            out.beginArray()
            out.value(value.power)
            out.value(value.motion)
            out.value(value.equals)
            out.endArray()
        }

        @Throws(IOException::class)
        override fun read(`in`: JsonReader): Limit? {
            if (`in`.peek() == JsonToken.NULL) {
                `in`.nextNull()
                return null
            }

            `in`.beginArray()
            val obj = Limit(`in`.nextDouble().toFloat(), `in`.nextDouble().toFloat(), `in`.nextBoolean())
            `in`.endArray()

            return obj
        }
    }
}
