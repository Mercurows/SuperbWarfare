package com.atsuishio.superbwarfare.data.gun.value

import net.minecraft.nbt.CompoundTag

/**
 * 标记某种状态是否应该开始
 */
open class Starter(private val tag: CompoundTag, name: String) {
    private val name: String = "Start$name"

    /**
     * 检测当前状态是否应该开始
     */
    open fun shouldStart(): Boolean {
        return tag.getBoolean(name)
    }

    /**
     * 将当前状态设置为开始
     */
    open fun markStart() {
        tag.putBoolean(name, true)
    }

    /**
     * 将当前状态设置为结束
     */
    open fun finish() {
        tag.remove(name)
    }

    /**
     * 检测阶段是否应该开始，返回当前状态，并设置为结束
     */
    fun start(): Boolean {
        if (shouldStart()) {
            finish()
            return true
        }
        return false
    }
}
