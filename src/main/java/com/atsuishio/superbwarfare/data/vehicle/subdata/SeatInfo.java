package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.google.gson.annotations.SerializedName;

public class SeatInfo {
    @SerializedName("HidePassenger")
    public boolean hidePassenger = false;

    @SerializedName("IsEnclosed")
    @ServerOnly
    public Boolean isEnclosed = null;

    // TODO 武器
    // TODO 座位锚点
}
