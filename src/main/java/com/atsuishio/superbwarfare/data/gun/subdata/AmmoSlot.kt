package com.atsuishio.superbwarfare.data.gun.subdata

import net.minecraft.nbt.CompoundTag

class AmmoSlot(private val tag: CompoundTag) {

    private val slot: CompoundTag
        get() = tag.getCompound(AMMO_SLOT)

    private fun getOrCreateSlot(): CompoundTag {
        if (!tag.contains(AMMO_SLOT)) {
            tag.put(AMMO_SLOT, CompoundTag())
        }
        return this.slot
    }

    fun getAmmo(slot: String): Int {
        val arr = this.slot.getIntArray(slot)
        return if (arr.size > 0) arr[0] else 0
    }

    fun getVirtualAmmo(slot: String): Int {
        val arr = this.slot.getIntArray(slot)
        return if (arr.size > 1) arr[1] else 0
    }

    fun set(slot: String, ammo: Int, virtualAmmo: Int) {
        if (ammo <= 0 && virtualAmmo <= 0) {
            reset(slot)
        } else {
            val arr = intArrayOf(ammo, virtualAmmo)
            this.getOrCreateSlot().putIntArray(slot, arr)
        }
    }

    fun reset(slot: String) {
        val slotTag = this.slot
        slotTag.remove(slot)

        if (slotTag.isEmpty) {
            tag.remove(AMMO_SLOT)
        }
    }

    fun reset() {
        tag.remove(AMMO_SLOT)
    }

    companion object {
        const val AMMO_SLOT: String = "AmmoSlot"
    }
}
