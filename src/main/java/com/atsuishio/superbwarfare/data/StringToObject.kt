package com.atsuishio.superbwarfare.data

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.serialization.Serializable
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * "" -> {}
 */
// TODO 重新用kt实现STO
@Serializable
class StringToObject<T : DeserializeFromString>(@JvmField var value: T) {
    internal class StringOrObjectAdapter<T : DeserializeFromString>(type: Type, private val gson: Gson) :
        TypeAdapter<StringToObject<T>>() {
        /**
         * Type of T
         */
        private val type = (type as ParameterizedType).actualTypeArguments[0]

        @Throws(IOException::class)
        override fun write(jsonWriter: JsonWriter, obj: StringToObject<T>?) {
            if (obj == null) {
                jsonWriter.nullValue()
                return
            }

            gson.toJson(obj.value, type, jsonWriter)
        }

        @Throws(IOException::class)
        override fun read(jsonReader: JsonReader): StringToObject<T> {
            val token = jsonReader.peek()
            if (token == JsonToken.NULL) {
                jsonReader.nextNull()
                return gson.fromJson<StringToObject<T>>("{}", type)
            }

            if (token == JsonToken.BEGIN_OBJECT || token == JsonToken.BEGIN_ARRAY) {
                return StringToObject(gson.fromJson<T>(jsonReader, type))
            }

            val obj = gson.fromJson<T>("{}", type)
            obj!!.deserializeFromString(gson.fromJson(jsonReader, String::class.java))

            return StringToObject(obj)
        }
    }

    internal class AdapterFactory : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            if (StringToObject::class.java.isAssignableFrom(type.getRawType()) && type.type is ParameterizedType) {
                @Suppress("UNCHECKED_CAST")
                return StringOrObjectAdapter<DeserializeFromString>(type.type, gson) as TypeAdapter<T>
            }
            return null
        }
    }
}

//class STOSerializer<T>(private val serializer: KSerializer<StringToObject<T>>) : KSerializer<StringToObject<T>> {
//    override val descriptor: SerialDescriptor
//        get() = serializer.descriptor
//
//    override fun serialize(encoder: Encoder, value: StringToObject<T>) {
//        TODO("Not yet implemented")
//    }
//
//    override fun deserialize(decoder: Decoder): StringToObject<T> {
//        require(decoder is JsonDecoder)
//        val element = decoder.decodeJsonElement()
//
//        if (element !is JsonPrimitive || !element.jsonPrimitive.isString) return serializer.deserialize(decoder)
//
//        val fac = serializer.descriptor.annotations.filterIsInstance<STOFactory>().first().factory as KClass<StringInstanceBuilder<T>>
//        val obj = fac.objectInstance!!.fromString(element.jsonPrimitive.content)
//
//        return StringToObject(obj)
//    }
//
//}
