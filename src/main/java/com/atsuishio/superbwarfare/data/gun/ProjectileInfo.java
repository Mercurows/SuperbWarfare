package com.atsuishio.superbwarfare.data.gun;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ProjectileInfo {

    @SerializedName("Type")
    public String type = "superbwarfare:projectile";

    @SerializedName("Data")
    public JsonObject data;
}
