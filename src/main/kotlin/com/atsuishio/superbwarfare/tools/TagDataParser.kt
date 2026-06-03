package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.Mod
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.nbt.*
import java.util.function.Function

object TagDataParser {
    /**
     * 将JsonObject转换为NBT Tag，并替换自定义数据
     * 
     * @param object      JsonObject
     * @param tagModifier 替换函数
     * @return 替换后的NBT Tag
     */
    @JvmOverloads
    @JvmStatic
    fun parse(`object`: JsonObject?, tagModifier: Function<String, Tag?>? = null): CompoundTag {
        val tag = CompoundTag()
        if (`object` == null) return tag

        for (d in `object`.entrySet()) {
            try {
                val parsed = parse(d.value, tagModifier) ?: continue
                tag.put(d.key, parsed)
            } catch (e: Exception) {
                Mod.LOGGER.error("Failed to parse tag {}: {}", d.key, e)
            }
        }

        return tag
    }

    /**
     * 尝试将单个JsonElement转为NBT Tag，并替换自定义数据
     * 
     * @param object      JsonElement
     * @param tagModifier 替换函数
     * @return 替换后的NBT Tag
     */
    @JvmStatic
    fun parse(`object`: JsonElement, tagModifier: Function<String, Tag?>?): Tag? {
        if (`object`.isJsonObject) {
            // 递归处理嵌套内容
            val tag = CompoundTag()
            for (d in `object`.getAsJsonObject().entrySet()) {
                try {
                    val parsed = parse(d.value, tagModifier) ?: continue
                    tag.put(d.key, parsed)
                } catch (e: Exception) {
                    Mod.LOGGER.error("Failed to parse tag {}: {}", d.key, e)
                }
            }
            return tag
        } else if (`object`.isJsonArray) {
            // 处理数组相关内容
            val tag = ListTag()
            for (d in `object`.getAsJsonArray()) {
                tag.add(parse(d, tagModifier))
            }
            return tag
        } else if (`object`.isJsonPrimitive) {
            // 处理基础数据
            val prime = `object`.getAsJsonPrimitive()
            if (prime.isString) {
                // 替换自定义数据
                if (tagModifier != null) {
                    val tag = tagModifier.apply(prime.getAsString())
                    if (tag != null) return tag
                }
                return StringTag.valueOf(prime.getAsString())
            } else if (prime.isNumber) {
                return DoubleTag.valueOf(prime.asLong.toDouble())
            } else if (prime.isBoolean) {
                return ByteTag.valueOf(prime.getAsBoolean())
            }
            return null
        }
        return null
    }
}
