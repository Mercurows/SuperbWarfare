package com.atsuishio.superbwarfare.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder

/**
 * 创建一个List包装类，反序列化时将单个对象解析为单元素List，或直接以List方式进行读取，不影响序列化
 * {} -> [{}]
 */
@Serializable(OTLSerializer::class)
@Suppress("DelegationToVarProperty")
data class ObjectToList<T>(@JvmField var list: MutableList<T>) : List<T> by list {
    @SafeVarargs
    constructor(vararg objects: T) : this(mutableListOf(*objects))

}

class OTLSerializer<T>(val elementSerializer: KSerializer<T>) : KSerializer<ObjectToList<T>> {
    override val descriptor = elementSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: ObjectToList<T>
    ) {
        encoder.encodeSerializableValue(ListSerializer(elementSerializer), value.list)
    }

    override fun deserialize(decoder: Decoder): ObjectToList<T> {
        require(decoder is JsonDecoder) { "only JsonDecoder is supported!" }

        val element = decoder.decodeJsonElement()
        return if (element is JsonArray) {
            ObjectToList(element.map { decoder.json.decodeFromJsonElement(elementSerializer, it) }.toMutableList())
        } else {
            ObjectToList(listOf(decoder.json.decodeFromJsonElement(elementSerializer, element)).toMutableList())
        }
    }

}
