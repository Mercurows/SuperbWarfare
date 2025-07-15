package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class TagDataParser {
    /**
     * 将JsonObject转换为NBT Tag
     */
    public static CompoundTag parse(@Nullable JsonObject object) {
        return parse(object, null);
    }

    /**
     * 将JsonObject转换为NBT Tag，并替换自定义数据
     *
     * @param object      JsonObject
     * @param tagModifier 替换函数
     * @return 替换后的NBT Tag
     */
    public static CompoundTag parse(@Nullable JsonObject object, @Nullable Function<String, Tag> tagModifier) {
        var tag = new CompoundTag();
        if (object == null) return tag;

        for (var d : object.entrySet()) {
            try {
                var parsed = parse(d.getValue(), tagModifier);
                if (parsed == null) continue;
                tag.put(d.getKey(), parsed);
            } catch (Exception e) {
                Mod.LOGGER.error("Failed to parse tag {}: {}", d.getKey(), e);
            }
        }

        return tag;
    }

    /**
     * 尝试将单个JsonElement转为NBT Tag，并替换自定义数据
     *
     * @param object      JsonElement
     * @param tagModifier 替换函数
     * @return 替换后的NBT Tag
     */
    public static @Nullable Tag parse(@NotNull JsonElement object, @Nullable Function<String, Tag> tagModifier) {
        if (object.isJsonObject()) {
            // 递归处理嵌套内容
            var tag = new CompoundTag();
            for (var d : object.getAsJsonObject().entrySet()) {
                try {
                    var parsed = parse(d.getValue(), tagModifier);
                    if (parsed == null) continue;
                    tag.put(d.getKey(), parsed);
                } catch (Exception e) {
                    Mod.LOGGER.error("Failed to parse tag {}: {}", d.getKey(), e);
                }
            }
            return tag;
        } else if (object.isJsonArray()) {
            // 处理数组相关内容
            var tag = new ListTag();
            for (var d : object.getAsJsonArray()) {
                tag.add(parse(d, tagModifier));
            }
            return tag;
        } else if (object.isJsonPrimitive()) {
            // 处理基础数据
            var prime = object.getAsJsonPrimitive();
            if (prime.isString()) {
                // 替换自定义数据
                if (tagModifier != null) {
                    var tag = tagModifier.apply(prime.getAsString());
                    if (tag != null) return tag;
                }
                return StringTag.valueOf(prime.getAsString());
            } else if (prime.isNumber()) {
                return DoubleTag.valueOf(prime.getAsLong());
            } else if (prime.isBoolean()) {
                return ByteTag.valueOf(prime.getAsBoolean());
            }
            return null;
        }
        return null;
    }
}
