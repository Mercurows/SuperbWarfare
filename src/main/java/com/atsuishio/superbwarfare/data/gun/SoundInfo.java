package com.atsuishio.superbwarfare.data.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class SoundInfo {

    // 正常的开火音效
    @SerializedName("Fire1P")
    public String fire1P = "";
    @SerializedName("Fire3P")
    public String fire3P = "";
    @SerializedName("Fire3PFar")
    public String fire3PFar = "";
    @SerializedName("Fire3PVeryFar")
    public String fire3PVeryFar = "";

    // 装备消音器时的开火音效
    @SerializedName("Fire1PSilent")
    public String fire1PSilent = "";
    @SerializedName("Fire3PSilent")
    public String fire3PSilent = "";
    @SerializedName("Fire3PFarSilent")
    public String fire3PFarSilent = "";
    @SerializedName("Fire3PVeryFarSilent")
    public String fire3PVeryFarSilent = "";

    // 换弹音效
    @SerializedName("ReloadNormal")
    public String reloadNormal = "";
    @SerializedName("ReloadEmpty")
    public String reloadEmpty = "";
    @SerializedName("ReloadPrepare")
    public String reloadPrepare = "";
    @SerializedName("ReloadLoop")
    public String reloadLoop = "";
    @SerializedName("ReloadEnd")
    public String reloadEnd = "";

    @SerializedName("Bolt")
    public String bolt = "";

    @SerializedName("Change")
    public String change = "";

    @SerializedName("Locking")
    public String locking = "";
    @SerializedName("Locked")
    public String locked = "";

    public static SoundEvent getSoundEvent(String path) {
        if (path.isEmpty()) {
            return SoundEvents.EMPTY;
        }

        ResourceLocation location = ResourceLocation.tryParse(path);
        if (location == null) {
            return SoundEvents.EMPTY;
        }

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(location);
        if (sound == null) {
            return SoundEvents.EMPTY;
        }

        return sound;
    }
}
