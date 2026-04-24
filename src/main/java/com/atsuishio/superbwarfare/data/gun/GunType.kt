package com.atsuishio.superbwarfare.data.gun

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GunType {
    // 步枪
    @SerializedName("Rifle")
    @SerialName("Rifle")
    RIFLE,

    // 霰弹枪
    @SerializedName("Shotgun")
    @SerialName("Shotgun")
    SHOTGUN,

    // 狙击枪
    @SerializedName("Sniper")
    @SerialName("Sniper")
    SNIPER,

    // 机枪
    @SerializedName("MachineGun")
    @SerialName("MachineGun")
    MACHINE_GUN,

    // 手枪
    @SerializedName("Handgun")
    @SerialName("Handgun")
    HANDGUN,

    // 冲锋枪
    @SerializedName("Smg")
    @SerialName("Smg")
    SMG,

    // 直射发射器（例如火箭等）
    @SerializedName("DirectLauncher")
    @SerialName("DirectLauncher")
    DIRECT_LAUNCHER,

    // 曲射发射器（例如榴弹等）
    @SerializedName("CurvedLauncher")
    @SerialName("CurvedLauncher")
    CURVED_LAUNCHER,

    // 特殊武器
    @SerializedName("Special")
    @SerialName("Special")
    SPECIAL
}
