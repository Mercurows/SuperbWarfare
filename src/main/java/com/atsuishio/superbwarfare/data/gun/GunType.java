package com.atsuishio.superbwarfare.data.gun;

import com.google.gson.annotations.SerializedName;

public enum GunType {
    // 步枪
    @SerializedName("Rifle") RIFLE,
    // 霰弹枪
    @SerializedName("Shotgun") SHOTGUN,
    // 狙击枪
    @SerializedName("Sniper") SNIPER,
    // 机枪
    @SerializedName("MachineGun") MACHINE_GUN,
    // 手枪
    @SerializedName("Handgun") HANDGUN,
    // 冲锋枪
    @SerializedName("Smg") SMG,
    // 直射发射器（例如火箭等）
    @SerializedName("DirectLauncher") DIRECT_LAUNCHER,
    // 曲射发射器（例如榴弹等）
    @SerializedName("CurvedLauncher") CURVED_LAUNCHER,
    // 特殊武器
    @SerializedName("Special") SPECIAL
}
