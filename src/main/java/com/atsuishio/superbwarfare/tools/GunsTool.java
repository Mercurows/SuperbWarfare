package com.atsuishio.superbwarfare.tools;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

public class GunsTool {

    public static double getGunDoubleTag(final CompoundTag tag, String name) {
        return getGunDoubleTag(tag, name, 0);
    }

    public static double getGunDoubleTag(final CompoundTag tag, String name, double defaultValue) {
        var data = tag.getCompound("GunData");
        if (!data.contains(name)) return defaultValue;
        return data.getDouble(name);
    }

    @Nullable
    public static UUID getGunUUID(final CompoundTag tag) {
        if (!tag.contains("GunData")) return null;

        CompoundTag data = tag.getCompound("GunData");
        if (!data.hasUUID("UUID")) return null;
        return data.getUUID("UUID");
    }
}
