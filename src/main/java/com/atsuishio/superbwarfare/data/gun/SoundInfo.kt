package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.annotation.ServerOnly
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents

@Serializable
class SoundInfo {
    // 正常的开火音效
    @SerializedName("Fire1P")
    @SerialName("Fire1P")
    var fire1P: SerializedSoundEvent? = null

    @JvmField
    @ServerOnly
    @SerializedName("Fire3P")
    @SerialName("Fire3P")
    var fire3P: SerializedSoundEvent? = null

    @JvmField
    @ServerOnly
    @SerializedName("Fire3PFar")
    @SerialName("Fire3PFar")
    var fire3PFar: SerializedSoundEvent? = null

    @ServerOnly
    @SerializedName("Fire3PVeryFar")
    @SerialName("Fire3PVeryFar")
    var fire3PVeryFar: SerializedSoundEvent? = null

    // 装备消音器时的开火音效
    @SerializedName("Fire1PSilent")
    @SerialName("Fire1PSilent")
    var fire1PSilent: SerializedSoundEvent? = null

    @ServerOnly
    @SerializedName("Fire3PSilent")
    @SerialName("Fire3PSilent")
    var fire3PSilent: SerializedSoundEvent? = null

    @ServerOnly
    @SerializedName("Fire3PFarSilent")
    @SerialName("Fire3PFarSilent")
    var fire3PFarSilent: SerializedSoundEvent? = null

    @ServerOnly
    @SerializedName("Fire3PVeryFarSilent")
    @SerialName("Fire3PVeryFarSilent")
    var fire3PVeryFarSilent: SerializedSoundEvent? = null

    // 换弹音效
    @SerializedName("ReloadNormal")
    @SerialName("ReloadNormal")
    var reloadNormal: SerializedSoundEvent? = null

    @SerializedName("ReloadEmpty")
    @SerialName("ReloadEmpty")
    var reloadEmpty: SerializedSoundEvent? = null

    @JvmField
    @SerializedName("VehicleReload")
    @SerialName("VehicleReload")
    var vehicleReload: SerializedSoundEvent = SoundEvents.EMPTY

    @SerializedName("VehicleReload3p")
    @SerialName("VehicleReload3p")
    var vehicleReload3p: SerializedSoundEvent = SoundEvents.EMPTY

    @SerializedName("VehicleReloadSoundTime")
    @SerialName("VehicleReloadSoundTime")
    var vehicleReloadSoundTime: Int = 0

    @SerializedName("ReloadPrepare")
    @SerialName("ReloadPrepare")
    var reloadPrepare: SerializedSoundEvent? = null

    @SerializedName("ReloadPrepareEmpty")
    @SerialName("ReloadPrepareEmpty")
    var reloadPrepareEmpty: SerializedSoundEvent? = null

    @SerializedName("ReloadPrepareLoad")
    @SerialName("ReloadPrepareLoad")
    var reloadPrepareLoad: SerializedSoundEvent? = null

    @SerializedName("ReloadLoop")
    @SerialName("ReloadLoop")
    var reloadLoop: SerializedSoundEvent? = null

    @SerializedName("ReloadEnd")
    @SerialName("ReloadEnd")
    var reloadEnd: SerializedSoundEvent? = null

    @SerializedName("Bolt")
    @SerialName("Bolt")
    var bolt: SerializedSoundEvent? = null

    @SerializedName("Change")
    @SerialName("Change")
    var change: SerializedSoundEvent? = null

    @SerializedName("Locking")
    @SerialName("Locking")
    var locking: SerializedSoundEvent = SoundEvents.EMPTY

    @SerializedName("Locked")
    @SerialName("Locked")
    var locked: SerializedSoundEvent = SoundEvents.EMPTY

    @SerializedName("FireSoundInstances")
    @SerialName("FireSoundInstances")
    var fireSoundInstances: SerializedSoundEvent? = null

    // 切枪时应该被中止播放的音效
    @SerializedName("CancellableSounds")
    @SerialName("CancellableSounds")
    var cancellableSounds: ObjectToList<String> = ObjectToList()
}
