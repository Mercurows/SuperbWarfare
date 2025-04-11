package com.atsuishio.superbwarfare.item.gun.data.subdata;

import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import net.minecraft.nbt.CompoundTag;

public final class Attachment {
    private final CompoundTag attachment;

    public Attachment(GunData gun) {
        this.attachment = gun.attachment();
    }

    public int get(AttachmentType type) {
        return attachment.getInt(type.getName());
    }

    public void set(AttachmentType type, int value) {
        attachment.putInt(type.getName(), value);
    }
}
