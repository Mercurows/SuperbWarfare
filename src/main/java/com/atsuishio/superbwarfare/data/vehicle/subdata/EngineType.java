package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public enum EngineType {
    @SerializedName("Empty") EMPTY,
    @SerializedName("Fixed") FIXED,
    @SerializedName("Wheel") WHEEL,
    @SerializedName("Track") TRACK,
    @SerializedName("Helicopter") HELICOPTER,
    @SerializedName("Ship") SHIP,
    @SerializedName("Aircraft") AIRCRAFT,
    @SerializedName("WheelChair") WHEELCHAIR
}
