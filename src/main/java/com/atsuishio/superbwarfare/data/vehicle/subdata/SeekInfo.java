package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public class SeekInfo {

    @SerializedName("MaxSeekRange")
    public double maxSeekRange = 64;

    @SerializedName("MinSeekRange")
    public double minSeekRange = 1;

    @SerializedName("ChangeTargetTime")
    public int changeTargetTime = 60;

    @SerializedName("SeekIterative")
    public int seekIterative = 20;

    @SerializedName("MinTargetSize")
    public double minTargetSize = 0.25;
}
