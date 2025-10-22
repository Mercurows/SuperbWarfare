package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.ObjectToList;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    @SerializedName("ReloadPrepareEmpty")
    public String reloadPrepareEmpty = "";
    @SerializedName("ReloadPrepareLoad")
    public String reloadPrepareLoad = "";
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

    // 切枪时应该被中止播放的音效
    @SerializedName("CancellableSounds")
    public ObjectToList<String> cancellableSounds = new ObjectToList<>();

    @Nullable
    public SoundEvent getSoundEvent(@NotNull String path) {
        if (path.isEmpty()) {
            return null;
        }

        ResourceLocation location = ResourceLocation.tryParse(path);
        if (location == null) {
            return null;
        }

        return ForgeRegistries.SOUND_EVENTS.getValue(location);
    }
}
