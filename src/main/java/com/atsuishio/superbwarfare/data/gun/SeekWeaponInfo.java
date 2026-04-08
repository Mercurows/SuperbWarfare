package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;

public class SeekWeaponInfo {
    @SerializedName("SeekDirection")
    public StringOrVec3 seekDirection = new StringOrVec3("Default");
    @SerializedName("SeekRange")
    public double seekRange = 384;

    @SerializedName("SeekAngle")
    public double seekAngle = 20;

    @SerializedName("MinTargetHeight")
    public double minTargetHeight = 0;

    @SerializedName("MaxTargetHeight")
    public double maxTargetHeight = 114514;

    @SerializedName("SeekTime")
    public int seekTime = 10;

    @SerializedName("MinTargetSize")
    public double minTargetSize = 0;

    @SerializedName("CalculateTrajectory")
    public boolean calculateTrajectory = false;

    @SerializedName("OnlyLockBlock")
    public boolean onlyLockBlock = false;

    @SerializedName("OnlyLockEntity")
    public boolean onlyLockEntity = false;

    @SerializedName("MaxGuidedRange")
    public double maxGuidedRange = 2048;

    @SerializedName("CanGuidedByRadar")
    public boolean canGuidedByRadar = true;

    @SerializedName("AffectedByStealthTarget")
    public boolean affectedByStealthTarget = true;
}
