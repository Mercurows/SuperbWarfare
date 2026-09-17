package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GunType {
    // 步枪
    @SerialName("Rifle")
    RIFLE,

    // 霰弹枪
    @SerialName("Shotgun")
    SHOTGUN,

    // 狙击枪
    @SerialName("Sniper")
    SNIPER,

    // 机枪
    @SerialName("MachineGun")
    MACHINE_GUN,

    // 手枪
    @SerialName("Handgun")
    HANDGUN,

    // 冲锋枪
    @SerialName("Smg")
    SMG,

    // 直射发射器（例如火箭等）
    @SerialName("DirectLauncher")
    DIRECT_LAUNCHER,

    // 曲射发射器（例如榴弹等）
    @SerialName("CurvedLauncher")
    CURVED_LAUNCHER,

    // 特殊武器
    @SerialName("Special")
    SPECIAL
}
