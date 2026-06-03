package com.atsuishio.superbwarfare.tools

import com.google.gson.*
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.*
import net.minecraft.world.item.ItemStack

object NBTTool {
    @JvmStatic
    fun getTag(stack: ItemStack): CompoundTag {
        val data = stack.get(DataComponents.CUSTOM_DATA)
        if (data != null) return data.copyTag()

        return CompoundTag()
    }

    @JvmStatic
    fun convertToJson(nbt: CompoundTag): JsonObject {
        val json = JsonObject()
        nbt.allKeys.forEach {
            val tag = nbt.get(it)
            json.add(it, parseTag(tag!!))
        }
        return json
    }

    // 处理单个 NBT 标签
    @JvmStatic
    fun parseTag(tag: Tag): JsonElement {
        when (tag) {
            is CompoundTag -> {
                return convertToJson(tag)
            }

            is ListTag -> {
                val array = JsonArray()
                tag.forEach { array.add(parseTag(it)) }
                return array
            }

            is NumericTag -> {
                return JsonPrimitive(tag.asNumber)
            }

            is StringTag -> {
                return JsonPrimitive(tag.asString)
            }

            is ByteArrayTag -> {
                val array = JsonArray()
                val bytes = tag.asByteArray
                for (b in bytes) array.add(JsonPrimitive(b))
                return array
            }

            is IntArrayTag -> {
                val array = JsonArray()
                val ints = tag.asIntArray
                for (i in ints) array.add(JsonPrimitive(i))
                return array
            }

            is LongArrayTag -> {
                val array = JsonArray()
                val longs = tag.asLongArray
                for (l in longs) array.add(JsonPrimitive(l))
                return array
            }

            is EndTag -> {
                return JsonNull.INSTANCE // 处理结束标签
            }
            // 未知类型回退到字符串表示
            else -> return JsonPrimitive(tag.toString())
        }
    }
}
