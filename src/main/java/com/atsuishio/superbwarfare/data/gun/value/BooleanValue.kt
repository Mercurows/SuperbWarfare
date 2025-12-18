package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.TagValue
import net.minecraft.nbt.CompoundTag

class BooleanValue(
    private val tag: CompoundTag,
    private val name: String,
    override val defaultValue: Boolean = false
) : TagValue<Boolean> {

    override fun get(): Boolean {
        if (tag.contains(name)) {
            return tag.getBoolean(name)
        }
        return defaultValue
    }

    override fun set(value: Boolean) {
        if (value == defaultValue) {
            tag.remove(name)
        } else {
            tag.putBoolean(name, value)
        }
    }

    override fun reset() {
        set(defaultValue)
    }
}
