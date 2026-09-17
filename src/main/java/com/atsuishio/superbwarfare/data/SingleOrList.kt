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
@Serializable(SingleOrListSerializer::class)
@Suppress("DelegationToVarProperty")
data class SingleOrList<T>(@JvmField var list: MutableList<T>) : List<T> by list {
    @SafeVarargs
    constructor(vararg objects: T) : this(mutableListOf(*objects))

}

class SingleOrListSerializer<T>(val elementSerializer: KSerializer<T>) : KSerializer<SingleOrList<T>> {
    override val descriptor = elementSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: SingleOrList<T>
    ) {
        encoder.encodeSerializableValue(ListSerializer(elementSerializer), value.list)
    }

    override fun deserialize(decoder: Decoder): SingleOrList<T> {
        require(decoder is JsonDecoder) { "only JsonDecoder is supported!" }

        val element = decoder.decodeJsonElement()
        return if (element is JsonArray) {
            SingleOrList(element.map { decoder.json.decodeFromJsonElement(elementSerializer, it) }.toMutableList())
        } else {
            SingleOrList(listOf(decoder.json.decodeFromJsonElement(elementSerializer, element)).toMutableList())
        }
    }

}
