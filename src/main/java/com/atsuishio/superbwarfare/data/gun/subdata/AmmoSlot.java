package com.atsuishio.superbwarfare.data.gun.subdata;

import net.minecraft.nbt.CompoundTag;

public final class AmmoSlot {
    public static final String AMMO_SLOT = "AmmoSlot";
    private final CompoundTag tag;

    public AmmoSlot(CompoundTag data) {
        this.tag = data;
    }

    private CompoundTag getSlot() {
        return tag.getCompound(AMMO_SLOT);
    }

    private CompoundTag getOrCreateSlot() {
        if (!tag.contains(AMMO_SLOT)) {
            tag.put(AMMO_SLOT, new CompoundTag());
        }
        return getSlot();
    }

    public int getAmmo(String slot) {
        var arr = getSlot().getIntArray(slot);
        return arr.length > 0 ? arr[0] : 0;
    }

    public int getVirtualAmmo(String slot) {
        var arr = getSlot().getIntArray(slot);
        return arr.length > 1 ? arr[1] : 0;
    }

    public void set(String slot, int ammo, int virtualAmmo) {
        if (ammo <= 0 && virtualAmmo <= 0) {
            reset(slot);
        } else {
            var arr = new int[]{ammo, virtualAmmo};
            getOrCreateSlot().putIntArray(slot, arr);
        }
    }

    public void reset(String slot) {
        var slotTag = getSlot();
        slotTag.remove(slot);

        if (slotTag.isEmpty()) {
            tag.remove(AMMO_SLOT);
        }
    }

    public void reset() {
        tag.remove(AMMO_SLOT);
    }
}
