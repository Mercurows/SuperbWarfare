package com.atsuishio.superbwarfare.data

import java.lang.reflect.Type
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
abstract class Prop<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD, RESULT, SELF : Prop<DATA, DEFAULT_DATA, FIELD, RESULT, SELF>> protected constructor(
    val prop: KMutableProperty1<DEFAULT_DATA, FIELD>,
    val transform: (FIELD) -> RESULT,
    val limiter: PropModifyContext<DATA, DEFAULT_DATA, RESULT>.() -> RESULT = { value },
) {
    protected val type: Type = prop.returnType.javaType

    init {
        props.add(this)
    }

    fun getDefault(data: DEFAULT_DATA): RESULT {
        return transform(prop.get(data))
    }

    fun asModifier(data: DATA): PropModifier<DATA, DEFAULT_DATA, RESULT> {
        return PropModifier(this, data)
    }

    class PropModifyContext<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, VALUE>(
        val modifier: PropModifier<DATA, DEFAULT_DATA, VALUE>,
        val data: DATA,
        val value: VALUE
    )

    companion object {
        @JvmField
        val props = mutableListOf<Prop<*, *, *, *, *>>()
    }
}
