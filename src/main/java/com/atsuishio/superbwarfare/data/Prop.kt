package com.atsuishio.superbwarfare.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.lang.reflect.Type
import kotlin.reflect.KProperty1
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
abstract class Prop<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA, FIELD, RESULT, SELF : Prop<DATA, DEFAULT_DATA, FIELD, RESULT, SELF>> protected constructor(
    val prop: KProperty1<DEFAULT_DATA, FIELD>,
    private val transform: (FIELD) -> RESULT,
    private val contextTransform: ((DATA, FIELD) -> RESULT)? = null,
) {
    @JvmField
    val type: Type = prop.returnType.javaType

    val serializer by lazy { prop.serializer() }

    override fun toString() = "Prop[$serializationName]"

    val serializationName = prop.annotations.filterIsInstance<SerialName>().singleOrNull()?.value
        ?: prop.name

    init {
        props.add(this)
    }

    /** 从 [defaultData] 上读出该属性的计算值 */
    fun getDefault(data: DATA, defaultData: DEFAULT_DATA): RESULT {
        val field = prop.get(defaultData)
        return contextTransform?.invoke(data, field) ?: transform(field)
    }

    fun deserialize(data: DATA, element: JsonElement): RESULT {
        val field = Json.decodeFromJsonElement(serializer, element)
        return contextTransform?.invoke(data, field) ?: transform(field)
    }

    companion object {
        @JvmField
        val props = mutableListOf<Prop<*, *, *, *, *>>()
    }
}

/**
 * 属性计算上下文（PMC）。
 *
 * 它同时是**读取入口**（[get]）和**写入缓冲**（[set] / [modify]），但两者刻意分开存放：
 *
 * - [cachedReads]：从 [computed] 上读出来的原始值缓存，不表示"被改过"；
 * - [diff]：Perk / 配件 / 脚本**显式写入**的差量。
 *
 * 分开的原因：只有这样才能回答"这次计算到底改了哪些属性"（[dirtyProps]），
 * 而"把差量一次性写回成一份 DefaultXxxData"（[computed] 的下一次迭代）也依赖这份差量。
 *
 * [computed] 是本次计算的起点值，默认就是数据包基线；将来各层可以直接给它赋新值
 * （`computed = computed.copy(...)`，此时 [cachedReads] 会被清掉，但 [diff] 不会丢）。
 */
class PMC<DATA : DefaultDataSupplier<DEFAULT_DATA>, DEFAULT_DATA>(
    val data: DATA,
    computed: DEFAULT_DATA? = null,
) {

    /** 本次计算的起点；每次读取都基于它，而不是反复回共享基线 */
    var computed: DEFAULT_DATA = computed ?: data.getDefault()
        set(value) {
            field = value
            cachedReads.clear()
        }

    private val cachedReads = mutableMapOf<Prop<DATA, *, *, *, *>, Any?>()
    private val diff = mutableMapOf<Prop<DATA, *, *, *, *>, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, *>, RESULT> get(prop: T): RESULT {
        diff[prop]?.let { return it as RESULT }
        return cachedReads.getOrPut(prop) { prop.getDefault(data, computed) } as RESULT
    }

    operator fun <T : Prop<DATA, DEFAULT_DATA, *, RESULT, *>, RESULT> set(prop: T, value: RESULT) {
        diff[prop] = value
    }

    /** 本次计算中被显式改写的属性 */
    fun dirtyProps(): Set<Prop<DATA, *, *, *, *>> = diff.keys

    fun diffSnapshot(): Map<Prop<DATA, *, *, *, *>, Any?> = diff.toMap()

    fun reset() {
        cachedReads.clear()
        diff.clear()
        computed = data.getDefault()
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
