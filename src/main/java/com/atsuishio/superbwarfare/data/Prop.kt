package com.atsuishio.superbwarfare.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

fun <T, V> createKSerializer(field: KProperty1<T, V>): KSerializer<V> {
    val s = field.annotations.filterIsInstance<Serializable>().singleOrNull()?.with
    @Suppress("UNCHECKED_CAST")
    return createKSerializer(field.returnType, s) as KSerializer<V>
}

// TODO 牛魔的这annotation和kclass读取出的是个啥啊
fun createKSerializer(type: KType, serializerClass: KClass<out KSerializer<*>>? = null): KSerializer<*> {
    val s = serializerClass
        ?: type.annotations.filterIsInstance<Serializable>().singleOrNull()?.with
        ?: return serializer(type)

    // object
    val instance = s.objectInstance
    if (instance != null) {
        return instance
    }

    val types = type.arguments.mapNotNull { it.type }
    val noArgsConstructor = s.constructors.singleOrNull { it.parameters.all(KParameter::isOptional) }
    if (noArgsConstructor != null) {
        // 无参构造
        return s.createInstance()
    }

    // 类型参数serializer
    return s.primaryConstructor?.call(*types.map { createKSerializer(it) }.toTypedArray())
        ?: throw RuntimeException("primaryConstructor of $s == null")
}


@OptIn(ExperimentalStdlibApi::class)
abstract class Prop<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD, RESULT, SELF : Prop<DATA, DEFAULT_DATA, FIELD, RESULT, SELF>> protected constructor(
    val prop: KMutableProperty1<DEFAULT_DATA, FIELD>,
    val transform: (FIELD) -> RESULT,
) {
    protected val type: Type = prop.returnType.javaType

    val serializer by lazy { createKSerializer(prop) }

    init {
        props.add(this)
    }

    fun getDefault(data: DEFAULT_DATA): RESULT {
        return transform(prop.get(data))
    }

    companion object {
        @JvmField
        val props = mutableListOf<Prop<*, *, *, *, *>>()
    }
}

// TODO
// 属性修改上下文，可以视为针对当前类型属性的所有属性值的临时map
class PMC<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA>(val data: DATA) {

    private val currentProps = mutableMapOf<Prop<DATA, *, *, *, *>, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, T>, RESULT> get(prop: T) = currentProps.getOrPut(prop) {
        prop.getDefault(data.getDefault()) as Any?
    } as RESULT

    operator fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, T>, RESULT : Any> set(prop: T, value: RESULT) {
        currentProps[prop] = value
    }

    fun reset() {
        currentProps.clear()
    }

    fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, T>, RESULT : Any> modify(
        prop: T,
        modifier: (RESULT) -> RESULT
    ) {
        this[prop] = modifier(this[prop])
    }
}