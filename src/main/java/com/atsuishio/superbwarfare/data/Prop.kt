package com.atsuishio.superbwarfare.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.lang.reflect.Type
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
abstract class Prop<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD, RESULT, SELF : Prop<DATA, DEFAULT_DATA, FIELD, RESULT, SELF>> protected constructor(
    val prop: KMutableProperty1<DEFAULT_DATA, FIELD>,
    val transform: (FIELD) -> RESULT,
) {
    @JvmField
    val type: Type = prop.returnType.javaType

    val serializer by lazy { prop.serializer() }

    override fun toString() = "Prop[$serializationName]"

    val serializationName = prop.annotations.filterIsInstance<SerialName>().singleOrNull()?.value
        ?: prop.annotations.filterIsInstance<SerializedName>().singleOrNull()?.value
        ?: prop.name

    init {
        props.add(this)
    }

    fun getDefault(data: DEFAULT_DATA): RESULT {
        return transform(prop.get(data))
    }

    fun deserialize(element: JsonElement): RESULT {
        return transform(Json.decodeFromJsonElement(serializer, element))
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
    operator fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, *>, RESULT> get(prop: T) = currentProps.getOrPut(prop) {
        prop.getDefault(data.getDefault()) as Any?
    } as RESULT

    operator fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, *>, RESULT> set(prop: T, value: RESULT) {
        currentProps[prop] = value
    }

    fun reset() {
        currentProps.clear()
    }

    fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, *>, RESULT : Any> modify(
        prop: T,
        modifier: (RESULT) -> RESULT
    ) {
        this[prop] = modifier(this[prop])
    }

    @Suppress("UNCHECKED_CAST")
    fun getUnchecked(prop: Prop<*, *, *, *, *>): Any? {
        return (this as PMC<Any, Any?>)[prop as Prop<Any, Any?, *, Any?, *>]
    }

    @Suppress("UNCHECKED_CAST")
    fun setUnchecked(prop: Prop<*, *, *, *, *>, value: Any?) {
        (this as PMC<Any?, Any?>)[prop as Prop<Any?, Any?, *, Any?, *>] = value
    }
}