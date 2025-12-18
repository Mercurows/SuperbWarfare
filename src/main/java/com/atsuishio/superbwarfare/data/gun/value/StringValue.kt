package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.TagValue
import net.minecraft.nbt.CompoundTag

class StringValue(
    private val tag: CompoundTag,
    private val name: String,
    override val defaultValue: String = ""
) : TagValue<String> {

    override fun get(): String {
        if (tag.contains(name)) {
            return tag.getString(name)
        }
        return defaultValue
    }

    override fun set(value: String) {
        if (defaultValue == value) {
            tag.remove(name)
        } else {
            tag.putString(name, value)
        }
    }
}
