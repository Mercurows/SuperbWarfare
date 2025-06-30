package com.atsuishio.superbwarfare.data.drone_attachment;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class DroneAttachmentData implements IDBasedData {
    @SerializedName("ItemID")
    public String itemID = "";

    @SerializedName("EntityID")
    public String EntityID = "";

    @Override
    public String getId() {
        return this.itemID;
    }

    @SerializedName("Count")
    public int count = 1;

    @SerializedName("IsKamikaze")
    public boolean isKamikaze = true;

    @SerializedName("ExplosionDamage")
    public float explosionDamage = 0;

    @SerializedName("ExplosionRadius")
    public float explosionRadius = 0;

    // TODO 其他挂载设置

    // display settings

    @SerializedName("Scale")
    public float[] scale = new float[]{1, 1, 1};

    @SerializedName("Offset")
    public float[] offset = new float[]{0, 0, 0};

    @SerializedName("Rotation")
    public float[] rotation = new float[]{0, 0, 0};

    @SerializedName("Data")
    public JsonObject data;
}
