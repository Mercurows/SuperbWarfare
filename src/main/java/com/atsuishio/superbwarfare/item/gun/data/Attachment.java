package com.atsuishio.superbwarfare.item.gun.data;

import net.minecraft.nbt.CompoundTag;

public final class Attachment {
    private final CompoundTag attachment;

    Attachment(GunData gun) {
        this.attachment = gun.attachment();
    }

    public int get(AttachmentType type) {
        return attachment.getInt(type.getName());
    }

    public void set(AttachmentType type, int value) {
        attachment.putInt(type.getName(), value);
    }
}
