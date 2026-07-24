package com.atsuishio.superbwarfare.tools

import net.minecraft.nbt.CompoundTag

/**
 * Interface implemented by entities that support lightweight, direct NBT serialization
 * for Beyond Visual Range (BVR) network synchronization.
 *
 * <p>Implementing this interface completely bypasses vanilla disk-save serialization
 * ([net.minecraft.world.entity.Entity.serializeNBT]), preventing expensive inventory,
 * advancement, energy storage, or AI goal dumps during network sync ticks.
 *
 * @author superbwarfare contributors
 * @since 0.8.9.1
 */
interface IBvrSyncableEntity {

    /**
     * Writes minimal NBT fields required for long-range BVR client rendering and tracking.
     *
     * @param tag destination [CompoundTag] to write fields into.
     */
    fun buildBvrSyncNbt(tag: CompoundTag)
}