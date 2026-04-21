package com.atsuishio.superbwarfare.data

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * 创建一个List包装类，反序列化时将单个对象解析为单元素List，或直接以List方式进行读取，不影响序列化
 * {} -> [{}]
 */
@Serializable(OTLSerializer::class)
@Suppress("DelegationToVarProperty")
data class ObjectToList<T>(@JvmField var list: MutableList<T>) : List<T> by list {
    @SafeVarargs
    constructor(vararg objects: T) : this(mutableListOf(*objects))

    internal class ListOrObjectAdapter<T>(type: Type, private val gson: Gson) : TypeAdapter<ObjectToList<T>>() {
        /**
         * Type of T
         */
        private val type = (type as ParameterizedType).actualTypeArguments[0]

        @Throws(IOException::class)
        override fun write(jsonWriter: JsonWriter, objectToList: ObjectToList<T>?) {
            val list = objectToList?.list
            if (objectToList == null || list == null) {
                jsonWriter.beginArray().endArray()
                return
            }

            if (objectToList.list.size == 1) {
                gson.toJson(list[0], type, jsonWriter)
            } else {
                gson.toJson(
                    objectToList.list,
                    TypeToken.getParameterized(MutableList::class.java, type).type,
                    jsonWriter
                )
            }
        }

        @Throws(IOException::class)
        override fun read(jsonReader: JsonReader): ObjectToList<T> {
            val token = jsonReader.peek()
            if (token != JsonToken.BEGIN_ARRAY) {
                // 单元素
                if (token == JsonToken.NULL) {
                    jsonReader.nextNull()
                    return ObjectToList()
                }
                return ObjectToList(gson.fromJson<T>(jsonReader, type))
            } else {
                // 数组
                val listType = TypeToken.getParameterized(MutableList::class.java, type).type
                return ObjectToList(gson.fromJson<MutableList<T>>(jsonReader, listType))
            }
        }
    }

    internal class AdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (ObjectToList::class.java.isAssignableFrom(type.getRawType())) {
                @Suppress("UNCHECKED_CAST")
                return ListOrObjectAdapter<T>(type.type, gson) as TypeAdapter<T>
            }
            return null
        }
    }
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
