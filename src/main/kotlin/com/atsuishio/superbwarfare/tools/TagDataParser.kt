package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import kotlinx.serialization.json.*
import net.minecraft.nbt.*
import java.util.function.Function

/**
 * 把 JSON 树转成 NBT，并替换 `@sbw:xxx` 占位符。
 *
 * 主接口用 kotlinx 的 [JsonElement]（数据包数据都已经迁到 kotlinx）；
 * 另外保留一组 Gson 重载，给仍由原版 Gson 解析的调用方（研究配方 / 载具装配配方）使用。
 */
object TagDataParser {

    /**
     * 将JsonObject转换为NBT Tag，并替换自定义数据
     *
     * @param object      kotlinx JsonObject
     * @param tagModifier 替换函数
     * @return 替换后的NBT Tag
     */
    @JvmOverloads
    @JvmStatic
    fun parseObject(`object`: JsonObject?, tagModifier: Function<String, Tag?>? = null): CompoundTag {
        val tag = CompoundTag()
        if (`object` == null) return tag

        for ((key, value) in `object`) {
            try {
                val parsed = parseElement(value, tagModifier) ?: continue
                tag.put(key, parsed)
            } catch (e: Exception) {
                Mod.LOGGER.error("Failed to parse tag {}: {}", key, e)
            }
        }

        return tag
    }

    /** Gson 重载：供仍由原版 Gson 解析的配方数据使用 */
    @JvmOverloads
    @JvmStatic
    fun parseObject(
        `object`: com.google.gson.JsonObject?,
        tagModifier: Function<String, Tag?>? = null
    ): CompoundTag = parseObject(`object`?.toKxJson()?.jsonObject, tagModifier)

    /**
     * 尝试将单个JsonElement转为NBT Tag，并替换自定义数据
     */
    @JvmStatic
    fun parseElement(`object`: JsonElement, tagModifier: Function<String, Tag?>?): Tag? {
        return when (`object`) {
            is JsonObject -> {
                // 递归处理嵌套内容
                val tag = CompoundTag()
                for ((key, value) in `object`) {
                    try {
                        val parsed = parseElement(value, tagModifier) ?: continue
                        tag.put(key, parsed)
                    } catch (e: Exception) {
                        Mod.LOGGER.error("Failed to parse tag {}: {}", key, e)
                    }
                }
                tag
            }

            is JsonArray -> {
                // 处理数组相关内容
                val tag = ListTag()
                for (element in `object`) {
                    tag.add(parseElement(element, tagModifier))
                }
                tag
            }

            is JsonPrimitive -> {
                if (`object`.isString) {
                    // 替换自定义数据
                    if (tagModifier != null) {
                        val replaced = tagModifier.apply(`object`.content)
                        if (replaced != null) return replaced
                    }
                    StringTag.valueOf(`object`.content)
                } else {
                    // Gson 版用 asLong 截断取整，这里保持同样行为
                    `object`.booleanOrNull?.let { return ByteTag.valueOf(it) }
                    `object`.doubleOrNull?.let { return DoubleTag.valueOf(it.toLong().toDouble()) }
                    null
                }
            }

        }
    }

    /** Gson 重载：供仍由原版 Gson 解析的配方数据使用 */
    @JvmStatic
    fun parseElement(`object`: com.google.gson.JsonElement, tagModifier: Function<String, Tag?>?): Tag? =
        parseElement(`object`.toKxJson(), tagModifier)
}
