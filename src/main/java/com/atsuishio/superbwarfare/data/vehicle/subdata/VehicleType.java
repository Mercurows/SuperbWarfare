package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public enum VehicleType {
    @SerializedName("Empty") EMPTY,
    @SerializedName("Tank") TANK,
    @SerializedName("APC") APC,
    @SerializedName("AA") AA,
    @SerializedName("Airplane") AIRPLANE,
    @SerializedName("Helicopter") HELICOPTER,
    @SerializedName("Car") CAR,
    @SerializedName("Artillery") ARTILLERY,
    @SerializedName("Defense") DEFENSE,
    @SerializedName("Boat") BOAT,
    @SerializedName("Drone") DRONE,
    @SerializedName("Special") SPECIAL,
}
