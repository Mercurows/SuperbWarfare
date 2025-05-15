package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

public class DoubleValue {
    private final CompoundTag tag;
    private final String name;
    private final double defaultValue;

    public DoubleValue(CompoundTag tag, String name, double defaultValue) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public DoubleValue(CompoundTag tag, String name) {
        this(tag, name, 0);
    }

    public double get() {
        if (tag.contains(name)) {
            return tag.getDouble(name);
        }
        return defaultValue;
    }

    public void set(double value) {
        if (value == defaultValue) {
            tag.remove(name);
        } else {
            tag.putDouble(name, value);
        }
    }

    public void reset() {
        set(defaultValue);
    }
}
