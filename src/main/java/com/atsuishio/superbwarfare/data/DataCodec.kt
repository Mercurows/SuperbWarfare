package com.atsuishio.superbwarfare.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

/**
 * kotlinx 版的数据编解码桥。
 *
 * [IDBasedData] 是 Java 接口且带 Java 实现类（如 `DroneAttachmentData`），
 * Kotlin 接口的默认实现无法被 Java 实现类继承（除非改 jvm-default 编译选项），
 * 所以把实现放在这里，由接口的 default 方法转发。
 */
object DataCodec {

    /**
     * 按运行时具体类型解析 `@Serializable` serializer。
     * 用 `value.javaClass` 而不是泛型参数，这样子类（如 `DefaultVehicleData`）能拿到自己的 serializer。
     */
    @JvmStatic
    fun <T : Any> serializerOf(value: T): KSerializer<T> {
        @Suppress("UNCHECKED_CAST")
        return DataLoader.JSON.serializersModule.serializer(value.javaClass) as KSerializer<T>
    }

    @JvmStatic
    fun <T : Any> toJsonObject(value: T): JsonObject {
        val element = DataLoader.JSON.encodeToJsonElement(serializerOf(value), value)
        return element as? JsonObject
            ?: error("${value.javaClass.name} did not serialize to a JSON object")
    }

    @JvmStatic
    fun <T : Any> fromJsonObject(value: T, json: JsonObject): T {
        return DataLoader.JSON.decodeFromJsonElement(serializerOf(value), json)
    }

    /**
     * 把 [override] 的顶层键合并进 [base] 后重新解析，override 优先。
     *
     * 取代原先 `GSON.toJsonTree(rawData)` → 合并 → `GSON.fromJson(...)` 的 Gson 往返。
     */
    @JvmStatic
    fun <T : Any> mergeOverride(base: T, override: JsonObject): T {
        if (override.isEmpty()) return base
        return fromJsonObject(base, JsonObject(toJsonObject(base) + override))
    }
}
