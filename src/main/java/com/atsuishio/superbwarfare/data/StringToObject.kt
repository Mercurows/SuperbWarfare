package com.atsuishio.superbwarfare.data

import com.atsuishio.superbwarfare.serialization.serializersModule
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance

/**
 * 创建一个value的包装类，允许使用字符串创建对象，或者直接以对象形式解析JSON值，在序列化和反序列化时可以将该包装类视为不存在
 * "" -> {}
 *
 * 字符串形式依赖类型上的 [STOFactory]；其中 Gson 版 TypeAdapter 还会调用 [DeserializeFromString]
 * （只有仍在走 Gson 的数据集需要，如移动枪/配方）。
 */
@Serializable(STOSerializer::class)
class StringToObject<T : Any>(@JvmField var value: T) {
}

private val cachedInstances = mutableMapOf<KClass<*>, Any>()

// 获取object实例或者创建无参构造函数实例
@Suppress("UNCHECKED_CAST")
private fun <T : Any> KClass<T>.getInstance() = cachedInstances.getOrPut(this) {
    objectInstance ?: createInstance()
} as T

class STOSerializer<T : Any>(private val serializer: KSerializer<T>) :
    KSerializer<StringToObject<T>> {
    override val descriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: StringToObject<T>) {
        encoder.encodeSerializableValue(serializer, value.value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): StringToObject<T> {
        require(decoder is JsonDecoder) { "only JsonDecoder is supported!" }
        val element = decoder.decodeJsonElement()

        if (element !is JsonPrimitive || !element.jsonPrimitive.isString) return StringToObject(
            decoder.json.decodeFromJsonElement(serializer, element)
        )

        @Suppress("UNCHECKED_CAST")
        val fac = serializer.descriptor.annotations.filterIsInstance<STOFactory>()
            .singleOrNull()?.factory as KClass<StringInstanceBuilder<T>>?

        requireNotNull(fac) { "No factory found for ${serializer.descriptor.serialName}! Add a @STOFactory annotation to your target class!" }
        return StringToObject(fac.getInstance().fromString(element.jsonPrimitive.content))
    }
}

/**
 * 将该注解用于StringToObject<T>的T类上，用于指定生成T实例的StringInstanceBuilder<T>工厂类
 */
@OptIn(ExperimentalSerializationApi::class)
@Retention(AnnotationRetention.RUNTIME)
@SerialInfo
@Target(AnnotationTarget.CLASS)
annotation class STOFactory(val factory: KClass<out StringInstanceBuilder<*>>)

interface StringInstanceBuilder<T> {
    fun fromString(value: String): T
}

@Suppress("UNCHECKED_CAST")
fun <V> KProperty<V>.serializer() = serializersModule.serializer(returnType) as KSerializer<V>
