package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.google.gson.annotations.SerializedName;

public class SeatInfo {
    @SerializedName("HidePassenger")
    public boolean hidePassenger = false;

    @SerializedName("IsEnclosed")
    @ServerOnly
    public Boolean isEnclosed = null;

    @SerializedName("WeaponData")
    public DefaultGunData weaponData = null;

    // TODO 座位锚点
}
