package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.Mod;
import com.google.gson.annotations.SerializedName;

public enum FireMode {
    @SerializedName("Semi")
    SEMI("Semi"),
    @SerializedName("Burst")
    BURST("Burst"),
    @SerializedName("Auto")
    AUTO("Auto");

    public final String name;

    FireMode(String name) {
        this.name = name;
    }

    public static FireMode fromValue(String value) {
        for (var enumConstant : FireMode.values()) {
            if (enumConstant.toString().equals(value)) {
                return enumConstant;
            }
        }
        Mod.LOGGER.warn("No FireMode with value {}", value);
        return FireMode.SEMI;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
