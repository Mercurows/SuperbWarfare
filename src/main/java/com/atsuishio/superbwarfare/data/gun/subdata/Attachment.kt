package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType

/**
 * Manages attachment slot data for a single [GunData] instance.
 *
 * Every write through [set] increments structural version so that
 * PMC calculations are invalidated and rebuilt lazily on demand.
 *
 * @param gun the owning [GunData] instance.
 */
class Attachment(private val gun: GunData) {

    private val attachment = gun.attachment()

    /**
     * Returns the current index of [type]'s attachment slot.
     *
     * @param type the attachment slot to query.
     * @return slot index, or 0 if empty.
     */
    fun get(type: AttachmentType): Int = attachment.getInt(type.attachmentName)

    /**
     * Sets [value] to [type]'s attachment slot and invalidates structural PMC if changed.
     *
     * @param type slot to modify.
     * @param value new slot index.
     */
    fun set(type: AttachmentType, value: Int) {
        if (attachment.getInt(type.attachmentName) == value) return  // no-op: unchanged
        attachment.putInt(type.attachmentName, value)
        gun.nbtVersion.invalidateStructural()
    }
}