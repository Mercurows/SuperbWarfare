package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

public class IntValue {
    private final CompoundTag tag;
    private final String name;
    private final int defaultValue;

    public IntValue(CompoundTag tag, String name, int defaultValue) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public IntValue(CompoundTag tag, String name) {
        this(tag, name, 0);
    }

    public int get() {
        if (tag.contains(name)) {
            return tag.getInt(name);
        }
        return defaultValue;
    }

    public void set(int value) {
        if (value == defaultValue) {
            tag.remove(name);
        } else {
            tag.putInt(name, value);
        }
    }

    public void add(int value) {
        set(get() + value);
    }

    public void reset() {
        set(defaultValue);
    }
}
