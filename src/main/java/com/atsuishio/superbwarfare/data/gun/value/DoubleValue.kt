package com.atsuishio.superbwarfare.data.gun.value

import net.minecraft.nbt.CompoundTag

class DoubleValue(
    private val tag: CompoundTag,
    private val name: String,
    var defaultValue: Double = 0.0
) {
    fun get(): Double {
        if (tag.contains(name)) {
            return tag.getDouble(name)
        }
        return defaultValue
    }

    fun set(value: Double) {
        if (value == defaultValue) {
            tag.remove(name)
        } else {
            tag.putDouble(name, value)
        }
    }

    fun add(value: Double) {
        set(get() + value)
    }

    fun reset() {
        set(defaultValue)
    }
}
