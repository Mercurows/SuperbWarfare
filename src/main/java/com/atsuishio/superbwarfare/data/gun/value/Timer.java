package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

/**
 * 针对一种状态的计时器
 */
public class Timer {
    private final CompoundTag tag;
    public final String name;

    public Timer(CompoundTag tag, String name) {
        this.tag = tag;
        this.name = name + "Time";
    }

    public int get() {
        return tag.getInt(name);
    }

    public void set(int time) {
        if (time <= 0) {
            tag.remove(name);
        } else {
            tag.putInt(name, time);
        }
    }

    public void add(int time) {
        set(get() + time);
    }

    public void reduce() {
        add(-1);
    }

    public void reset() {
        set(0);
    }
}
