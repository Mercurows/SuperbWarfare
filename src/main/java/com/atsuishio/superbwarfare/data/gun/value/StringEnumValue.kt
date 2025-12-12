package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.TagValue
import net.minecraft.nbt.CompoundTag
import java.util.function.Function

open class StringEnumValue<T : Enum<T>>(
    private val tag: CompoundTag,
    private val name: String,
    override val defaultValue: T,
    private val toEnum: Function<String, T>
) : TagValue<T> {

    override fun get(): T {
        val value = if (tag.contains(name)) {
            tag.getString(name)
        } else {
            defaultValue.toString()
        }
        return toEnum.apply(value)
    }

    override fun set(value: T) {
        if (value === defaultValue) {
            tag.remove(name)
        } else {
            tag.putString(name, value.toString())
        }
    }
}
