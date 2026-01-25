package com.atsuishio.superbwarfare.data.gun.value

import net.minecraft.nbt.CompoundTag

class IntValue(
    private val tag: CompoundTag,
    private val name: String,
    var defaultValue: Int = 0
) {
    fun get(): Int {
        if (tag.contains(name)) {
            return tag.getInt(name)
        }
        return defaultValue
    }

    fun set(value: Int) {
        if (value == defaultValue) {
            tag.remove(name)
        } else {
            tag.putInt(name, value)
        }
    }

    fun add(value: Int) {
        set(get() + value)
    }

    fun reset() {
        set(defaultValue)
    }
}
