package com.atsuishio.superbwarfare.data.gun.subdata;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.annotations.SerializedName;

public final class GunIconInfo {

    @SerializedName("GunIcon")
    public String gunIcon = Mod.loc("textures/gun_icon/default_icon.png").toString();

    @SerializedName("VehicleGunIcon")
    public String vehicleGunIcon = Mod.loc("textures/screens/vehicle_weapon/empty.png").toString();
}
