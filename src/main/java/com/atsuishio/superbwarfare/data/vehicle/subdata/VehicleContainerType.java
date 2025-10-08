package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public enum VehicleContainerType {
    @SerializedName("Empty") EMPTY(0, 0),
    @SerializedName("One") ONE(1, 1),
    @SerializedName("Mini") MINI(1, 9),
    @SerializedName("Small") SMALL(3, 9),
    @SerializedName("Medium") MEDIUM(6, 9),
    @SerializedName("Large") LARGE(6, 13),
    @SerializedName("Huge") HUGE(6, 17),
    ;

    public final int row, col;

    VehicleContainerType(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getSize() {
        return row * col;
    }
}
