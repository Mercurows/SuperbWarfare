package com.atsuishio.superbwarfare.data.gun.value;

import net.minecraft.nbt.CompoundTag;

/**
 * 标记某种状态是否应该开始
 */
public class Starter {
    private final CompoundTag tag;
    private final String name;

    public Starter(CompoundTag tag, String name) {
        this.tag = tag;
        this.name = "Start" + name;
    }

    /**
     * 检测当前状态是否应该开始
     */
    public boolean shouldStart() {
        return tag.getBoolean(name);
    }

    /**
     * 将当前状态设置为开始
     */
    public void markStart() {
        tag.putBoolean(name, true);
    }

    /**
     * 将当前状态设置为结束
     */
    public void finish() {
        tag.remove(name);
    }

    /**
     * 检测阶段是否应该开始，返回当前状态，并设置为结束
     */
    public boolean start() {
        if (shouldStart()) {
            finish();
            return true;
        }
        return false;
    }
}
