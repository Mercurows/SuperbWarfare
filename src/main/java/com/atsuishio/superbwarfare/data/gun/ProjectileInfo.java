package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.FromString;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ProjectileInfo implements IDBasedData, FromString {

    @SerializedName("Type")
    public String type = "superbwarfare:projectile";

    @SerializedName("Data")
    public JsonObject data;

    @Override
    public String getId() {
        return type;
    }

    @Override
    public void fromString(String s) {
        this.type = s;
    }
}
