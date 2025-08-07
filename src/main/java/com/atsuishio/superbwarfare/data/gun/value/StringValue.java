package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

public class StringValue {
    private final CompoundTag tag;
    private final String name;
    private final String defaultValue;

    public StringValue(CompoundTag tag, String name, String defaultValue) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public StringValue(CompoundTag tag, String name) {
        this(tag, name, "");
    }

    public String get() {
        if (tag.contains(name)) {
            return tag.getString(name);
        }
        return defaultValue;
    }

    public void set(String value) {
        if (defaultValue.equals(value)) {
            tag.remove(name);
        } else {
            tag.putString(name, value);
        }
    }

    public void reset() {
        set(defaultValue);
    }
}
