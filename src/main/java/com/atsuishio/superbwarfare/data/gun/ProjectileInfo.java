package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ProjectileInfo implements IDBasedData<ProjectileInfo>, DeserializeFromString {

    @SerializedName("Type")
    public String type = "superbwarfare:projectile";

    @SerializedName("Data")
    public JsonObject data;

    @Override
    public String getId() {
        return type;
    }

    @Override
    public void setId(String id) {
        this.type = id;
    }

    @Override
    public void deserializeFromString(String str) {
        this.type = str;
    }
}
