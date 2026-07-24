package com.atsuishio.superbwarfare.data.gun.value

import com.atsuishio.superbwarfare.data.gun.value.base.TagValue
import net.minecraft.nbt.CompoundTag

/**
 * Stores a single boolean value backed by a [CompoundTag] entry.
 *
 * The key is omitted from the tag when the value equals [defaultValue].
 * An optional [onSet] callback is invoked whenever the stored value changes.
 *
 * @param tag the compound tag backing this value.
 * @param name the NBT key name.
 * @param defaultValue default boolean fallback.
 * @param onSet optional callback invoked when the stored boolean changes.
 */
class BooleanValue(
    private val tag: CompoundTag,
    private val name: String,
    override val defaultValue: Boolean = false,
    private val onSet: (() -> Unit)? = null
) : TagValue<Boolean> {

    /** Returns the stored value, or [defaultValue] if the key is absent. */
    override fun get(): Boolean = if (tag.contains(name)) tag.getBoolean(name) else defaultValue

    /**
     * Writes [value] to the tag and triggers [onSet] if changed.
     *
     * Early-exits without any NBT write when [value] equals the current stored value.
     */
    override fun set(value: Boolean) {
        val current = if (tag.contains(name)) tag.getBoolean(name) else defaultValue
        if (current == value) return                          // no-op: value unchanged
        if (value == defaultValue) tag.remove(name) else tag.putBoolean(name, value)
        onSet?.invoke()
    }

    /** Resets the stored value to [defaultValue]. */
    override fun reset() = set(defaultValue)
}