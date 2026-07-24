package com.atsuishio.superbwarfare.data.gun.value

import net.minecraft.nbt.CompoundTag

/**
 * Stores a single integer value backed by a [CompoundTag] entry.
 *
 * The key is omitted from the tag when the value equals [defaultValue],
 * keeping NBT payloads minimal.
 *
 * An optional [onSet] callback is invoked whenever the stored value
 * actually changes. Structural fields pass [com.atsuishio.superbwarfare.data.gun.NbtVersion.invalidateStructural];
 * state fields pass `null` to skip PMC invalidation entirely.
 *
 * @param tag the compound tag backing this value.
 * @param name the NBT key name.
 * @param defaultValue the fallback value when key is absent.
 * @param onSet optional callback invoked when value changes.
 */
class IntValue(
    private val tag: CompoundTag,
    private val name: String,
    var defaultValue: Int = 0,
    private val onSet: (() -> Unit)? = null
) {
    /** Returns the stored value, or [defaultValue] if the key is absent. */
    fun get(): Int = if (tag.contains(name)) tag.getInt(name) else defaultValue

    /**
     * Writes [value] to the tag and invokes [onSet] if the value changed.
     *
     * Early-exits without any NBT write or callback when [value] equals
     * the current stored value.
     *
     * @param value the new value to store.
     */
    fun set(value: Int) {
        val current = if (tag.contains(name)) tag.getInt(name) else defaultValue
        if (current == value) return
        if (value == defaultValue) tag.remove(name) else tag.putInt(name, value)
        onSet?.invoke()
    }

    /** Adds [value] to the current stored value. */
    fun add(value: Int) = set(get() + value)

    /** Resets the stored value to [defaultValue]. */
    fun reset() = set(defaultValue)

    /** Decrements the stored value by 1, floor 0. */
    fun reduce() = set(maxOf(get() - 1, 0))
}