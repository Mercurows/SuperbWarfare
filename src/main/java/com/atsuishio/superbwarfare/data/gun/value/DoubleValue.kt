package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.NumberValue
import net.minecraft.nbt.CompoundTag

class DoubleValue(
    private val tag: CompoundTag,
    private val name: String,
    override val defaultValue: Double = 0.0
) : NumberValue<Double> {

    override fun get(): Double {
        if (tag.contains(name)) {
            return tag.getDouble(name)
        }
        return defaultValue
    }

    override fun set(value: Double) {
        if (value == defaultValue) {
            tag.remove(name)
        } else {
            tag.putDouble(name, value)
        }
    }

    override fun add(value: Double) {
        set(get() + value)
    }
}
