package com.atsuishio.superbwarfare.data

import java.lang.reflect.Type
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
abstract class Prop<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD, RESULT, SELF : Prop<DATA, DEFAULT_DATA, FIELD, RESULT, SELF>> protected constructor(
    val prop: KMutableProperty1<DEFAULT_DATA, FIELD>,
    val transform: (FIELD) -> RESULT,
) {
    protected val type: Type = prop.returnType.javaType

    val serializer by lazy { prop.serializer() }

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