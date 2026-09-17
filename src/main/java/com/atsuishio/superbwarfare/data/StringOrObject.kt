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
 * 把一个字段的两种 JSON 写法统一成一个值：既可以是字符串简写（走 [StringOrObjectFactory] 指明的 builder），
 * 也可以是完整对象；序列化/反序列化时这个包装类相当于不存在。
 *
 * 值本身是不可变的（[value] 是 `val`），字段级可变性只存在于持有它的数据类里。
 */
@Serializable(StringOrObjectSerializer::class)
class StringOrObject<T : Any>(@JvmField val value: T)

private val cachedInstances = mutableMapOf<KClass<*>, Any>()

// 获取object实例或者创建无参构造函数实例
@Suppress("UNCHECKED_CAST")
private fun <T : Any> KClass<T>.getInstance() = cachedInstances.getOrPut(this) {
    objectInstance ?: createInstance()
} as T

class StringOrObjectSerializer<T : Any>(private val serializer: KSerializer<T>) :
    KSerializer<StringOrObject<T>> {
    override val descriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: StringOrObject<T>) {
        encoder.encodeSerializableValue(serializer, value.value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): StringOrObject<T> {
        require(decoder is JsonDecoder) { "only JsonDecoder is supported!" }
        val element = decoder.decodeJsonElement()

        if (element !is JsonPrimitive || !element.jsonPrimitive.isString) return StringOrObject(
            decoder.json.decodeFromJsonElement(serializer, element)
        )

        @Suppress("UNCHECKED_CAST")
        val fac = serializer.descriptor.annotations.filterIsInstance<StringOrObjectFactory>()
            .singleOrNull()?.factory as KClass<StringInstanceBuilder<T>>?

        requireNotNull(fac) { "No factory found for ${serializer.descriptor.serialName}! Add a @StringOrObjectFactory annotation to your target class!" }
        return StringOrObject(fac.getInstance().fromString(element.jsonPrimitive.content))
    }
}

/**
 * 将该注解用于 StringOrObject<T> 的 T 类上，用于指定生成T实例的StringInstanceBuilder<T>工厂类
 */
@OptIn(ExperimentalSerializationApi::class)
@Retention(AnnotationRetention.RUNTIME)
@SerialInfo
@Target(AnnotationTarget.CLASS)
annotation class StringOrObjectFactory(val factory: KClass<out StringInstanceBuilder<*>>)

interface StringInstanceBuilder<T> {
    fun fromString(value: String): T
}

@Suppress("UNCHECKED_CAST")
fun <V> KProperty<V>.serializer() = serializersModule.serializer(returnType) as KSerializer<V>
