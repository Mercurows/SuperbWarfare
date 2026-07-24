package com.atsuishio.superbwarfare.data.gun.value

import net.minecraft.nbt.CompoundTag

/**
 * Stores a single double value backed by a [CompoundTag] entry.
 *
 * The key is omitted from the tag when the value equals [defaultValue].
 * An optional [onSet] callback is invoked whenever the stored value changes.
 *
 * @param tag the compound tag backing this value.
 * @param name the NBT key name.
 * @param defaultValue default double value fallback.
 * @param onSet optional callback executed on actual value modification.
 */
class DoubleValue(
    private val tag: CompoundTag,
    private val name: String,
    var defaultValue: Double = 0.0,
    private val onSet: (() -> Unit)? = null
) {
    /** Returns the stored value, or [defaultValue] if the key is absent. */
    fun get(): Double = if (tag.contains(name)) tag.getDouble(name) else defaultValue

    /**
     * Writes [value] to the tag and triggers [onSet] if changed.
     *
     * Early-exits without any NBT write when [value] equals the current stored value.
     */
    fun set(value: Double) {
        val current = if (tag.contains(name)) tag.getDouble(name) else defaultValue
        if (current == value) return                          // no-op: value unchanged
        if (value == defaultValue) tag.remove(name) else tag.putDouble(name, value)
        onSet?.invoke()
    }

    /** Adds [value] to the current stored value. */
    fun add(value: Double) = set(get() + value)

    /** Resets the stored value to [defaultValue]. */
    fun reset() = set(defaultValue)
}