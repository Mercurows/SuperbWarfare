package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

import java.util.function.Function;

public class StringEnumValue<T> {
    private final CompoundTag tag;
    private final String name;
    private final T defaultValue;
    private final Function<String, T> toEnum;

    public StringEnumValue(CompoundTag tag, String name, T defaultValue, Function<String, T> toEnum) {
        this.tag = tag;
        this.name = name;
        this.defaultValue = defaultValue;
        this.toEnum = toEnum;
    }

    public T get() {
        String value;
        if (tag.contains(name)) {
            value = tag.getString(name);
        } else {
            value = defaultValue.toString();
        }
        return toEnum.apply(value);
    }

    public void set(T value) {
        if (value == defaultValue) {
            tag.remove(name);
        } else {
            tag.putString(name, value.toString());
        }
    }

    public void reset() {
        set(defaultValue);
    }

}
