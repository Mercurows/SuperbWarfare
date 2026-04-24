package com.atsuishio.superbwarfare.serialization.kserializer

import com.atsuishio.superbwarfare.tools.toGson
import com.atsuishio.superbwarfare.tools.toKxJson
import com.google.gson.JsonObject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

typealias SerializedGsonObject = @Serializable(GsonObjectSerializer::class) JsonObject

object GsonObjectSerializer : KSerializer<JsonObject> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor =
        SerialDescriptor("GsonObject", kotlinx.serialization.json.JsonObject.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: JsonObject) {
        require(encoder is JsonEncoder)
        encoder.encodeJsonElement(value.toKxJson())
    }

    override fun deserialize(decoder: Decoder): JsonObject {
        require(decoder is JsonDecoder)
        return decoder.decodeJsonElement().jsonObject.toGson().asJsonObject
    }
}