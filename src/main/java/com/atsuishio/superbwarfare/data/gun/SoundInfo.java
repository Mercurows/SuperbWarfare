package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.google.gson.annotations.SerializedName;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class SoundInfo {

    // 正常的开火音效
    @SerializedName("Fire1P")
    public SoundEvent fire1P;

    @ServerOnly
    @SerializedName("Fire3P")
    public SoundEvent fire3P;
    @ServerOnly
    @SerializedName("Fire3PFar")
    public SoundEvent fire3PFar;
    @ServerOnly
    @SerializedName("Fire3PVeryFar")
    public SoundEvent fire3PVeryFar;

    // 装备消音器时的开火音效
    @SerializedName("Fire1PSilent")
    public SoundEvent fire1PSilent;

    @ServerOnly
    @SerializedName("Fire3PSilent")
    public SoundEvent fire3PSilent;
    @ServerOnly
    @SerializedName("Fire3PFarSilent")
    public SoundEvent fire3PFarSilent;
    @ServerOnly
    @SerializedName("Fire3PVeryFarSilent")
    public SoundEvent fire3PVeryFarSilent;

    // 换弹音效
    @SerializedName("ReloadNormal")
    public SoundEvent reloadNormal;
    @SerializedName("ReloadEmpty")
    public SoundEvent reloadEmpty;

    @SerializedName("VehicleReload")
    public SoundEvent vehicleReload = SoundEvents.EMPTY;;

    @SerializedName("VehicleReload3p")
    public SoundEvent vehicleReload3p = SoundEvents.EMPTY;;

    @SerializedName("VehicleReloadSoundTime")
    public int vehicleReloadSoundTime = 0;

    @SerializedName("ReloadPrepare")
    public SoundEvent reloadPrepare;
    @SerializedName("ReloadPrepareEmpty")
    public SoundEvent reloadPrepareEmpty;
    @SerializedName("ReloadPrepareLoad")
    public SoundEvent reloadPrepareLoad;
    @SerializedName("ReloadLoop")
    public SoundEvent reloadLoop;
    @SerializedName("ReloadEnd")
    public SoundEvent reloadEnd;

    @SerializedName("Bolt")
    public SoundEvent bolt;

    @SerializedName("Change")
    public SoundEvent change;

    @SerializedName("Locking")
    public SoundEvent locking = SoundEvents.EMPTY;
    @SerializedName("Locked")
    public SoundEvent locked = SoundEvents.EMPTY;

    @SerializedName("FireSoundInstances")
    public SoundEvent fireSoundInstances;

    // 切枪时应该被中止播放的音效
    @SerializedName("CancellableSounds")
    public ObjectToList<String> cancellableSounds = new ObjectToList<>();

}
