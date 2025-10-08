package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public enum VehicleContainerType {
    @SerializedName("Empty") EMPTY(0, 0, false),
    @SerializedName("One") ONE(1, 1, false),
    @SerializedName("Mini") MINI(1, 9, true),
    @SerializedName("Small") SMALL(3, 9, true),
    @SerializedName("Medium") MEDIUM(6, 9, true),
    @SerializedName("Large") LARGE(6, 13, true),
    @SerializedName("Huge") HUGE(6, 17, true),
    @SerializedName("Special") SPECIAL(3, 4, false);

    private final int row, col;
    private final boolean hasMenu;

    VehicleContainerType(int row, int col, boolean hasMenu) {
        this.row = row;
        this.col = col;
        this.hasMenu = hasMenu;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean hasMenu() {
        return hasMenu;
    }

    public int getSize() {
        return row * col;
    }
}
