package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.NumberValue
import net.minecraft.nbt.CompoundTag

class IntValue(
    private val tag: CompoundTag,
    private val name: String,
    override var defaultValue: Int = 0
) : NumberValue<Int> {
    override fun get(): Int {
        if (tag.contains(name)) {
            return tag.getInt(name)
        }
        return defaultValue
    }

    override fun set(value: Int) {
        if (value == defaultValue) {
            tag.remove(name)
        } else {
            tag.putInt(name, value)
        }
    }

    override fun add(value: Int) {
        set(get() + value)
    }
}
