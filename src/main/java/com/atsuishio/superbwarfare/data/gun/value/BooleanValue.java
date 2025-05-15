package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

public class BooleanValue {
    private final CompoundTag tag;
    private final String name;
    private final boolean defaultValue;

    public BooleanValue(CompoundTag tag, String name, boolean defaultValue) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public BooleanValue(CompoundTag tag, String name) {
        this(tag, name, false);
    }

    public boolean get() {
        if (tag.contains(name)) {
            return tag.getBoolean(name);
        }
        return defaultValue;
    }

    public void set(boolean value) {
        if (value == defaultValue) {
            tag.remove(name);
        } else {
            tag.putBoolean(name, value);
        }
    }

    public void reset() {
        set(defaultValue);
    }
}
