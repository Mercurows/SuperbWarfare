package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents

@Serializable
data class SoundInfo(
    // 正常的开火音效
    @SerialName("Fire1P")
    val fire1P: SerializedSoundEvent? = null,

    @JvmField
    @SerialName("Fire3P")
    val fire3P: SerializedSoundEvent? = null,

    @JvmField
    @SerialName("Fire3PFar")
    val fire3PFar: SerializedSoundEvent? = null,

    @SerialName("Fire3PVeryFar")
    val fire3PVeryFar: SerializedSoundEvent? = null,

    // 装备消音器时的开火音效
    @SerialName("Fire1PSilent")
    val fire1PSilent: SerializedSoundEvent? = null,

    @SerialName("Fire3PSilent")
    val fire3PSilent: SerializedSoundEvent? = null,

    @SerialName("Fire3PFarSilent")
    val fire3PFarSilent: SerializedSoundEvent? = null,

    @SerialName("Fire3PVeryFarSilent")
    val fire3PVeryFarSilent: SerializedSoundEvent? = null,

    // 换弹音效
    @SerialName("ReloadNormal")
    val reloadNormal: SerializedSoundEvent? = null,

    @SerialName("ReloadEmpty")
    val reloadEmpty: SerializedSoundEvent? = null,

    @JvmField
    @SerialName("VehicleReload")
    val vehicleReload: SerializedSoundEvent = SoundEvents.EMPTY,

    @SerialName("VehicleReload3p")
    val vehicleReload3p: SerializedSoundEvent = SoundEvents.EMPTY,

    @SerialName("VehicleReloadSoundTime")
    val vehicleReloadSoundTime: Int = 0,

    @SerialName("ReloadPrepare")
    val reloadPrepare: SerializedSoundEvent? = null,

    @SerialName("ReloadPrepareEmpty")
    val reloadPrepareEmpty: SerializedSoundEvent? = null,

    @SerialName("ReloadPrepareLoad")
    val reloadPrepareLoad: SerializedSoundEvent? = null,

    @SerialName("ReloadLoop")
    val reloadLoop: SerializedSoundEvent? = null,

    @SerialName("ReloadEnd")
    val reloadEnd: SerializedSoundEvent? = null,

    @SerialName("Bolt")
    val bolt: SerializedSoundEvent? = null,

    @SerialName("Change")
    val change: SerializedSoundEvent? = null,

    @SerialName("Locking")
    val locking: SerializedSoundEvent = SoundEvents.EMPTY,

    @SerialName("Locked")
    val locked: SerializedSoundEvent = SoundEvents.EMPTY,

    @SerialName("FireSoundInstances")
    val fireSoundInstances: SerializedSoundEvent? = null,

    // 切枪时应该被中止播放的音效
    @SerialName("CancellableSounds")
    val cancellableSounds: ObjectToList<String> = ObjectToList(),
)
