package com.atsuishio.superbwarfare.perk.js

import net.minecraft.nbt.CompoundTag

class PerkTagProxy(private val tag: CompoundTag) {

    fun getInt(key: String): Int = tag.getInt(key)

    fun putInt(key: String, value: Int) = tag.putInt(key, value)

    fun getBoolean(key: String): Boolean = tag.getBoolean(key)

    fun putBoolean(key: String, value: Boolean) = tag.putBoolean(key, value)

    fun getShort(key: String): Short = tag.getShort(key)

    fun putShort(key: String, value: Short) = tag.putShort(key, value)

    fun getDouble(key: String): Double = tag.getDouble(key)

    fun putDouble(key: String, value: Double) = tag.putDouble(key, value)

    fun getString(key: String): String = tag.getString(key)

    fun putString(key: String, value: String) = tag.putString(key, value)

    fun has(key: String): Boolean = tag.contains(key)

    fun remove(key: String) = tag.remove(key)

    fun isEmpty(): Boolean = tag.isEmpty

    fun reduceCooldown(key: String) {
        if (!tag.contains(key)) return
        val next = tag.getInt(key) - 1
        if (next <= 0) {
            tag.remove(key)
        } else {
            tag.putInt(key, next)
        }
    }
}
