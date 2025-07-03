package com.atsuishio.superbwarfare.data.drone_attachment;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class DroneAttachmentData implements IDBasedData {
    @SerializedName("ItemID")
    public String itemID = "";

    @SerializedName("EntityID")
    public String entityID = "";

    @Override
    public String getId() {
        return this.itemID;
    }

    @SerializedName("Count")
    private int count = 1;

    public int count() {
        return isKamikaze ? 1 : Math.max(1, this.count);
    }

    @SerializedName("IsKamikaze")
    public boolean isKamikaze = true;

    @SerializedName("HitDamage")
    public float hitDamage = 0;

    @SerializedName("ExplosionDamage")
    public float explosionDamage = 0;

    @SerializedName("ExplosionRadius")
    public float explosionRadius = 0;

    /**
     * 投弹时需要写入的实体数据
     */
    @SerializedName("DropData")
    public JsonObject dropData;

    // TODO 其他挂载设置

    // display settings

    @SerializedName("Scale")
    private float[] scale = new float[]{1, 1, 1};

    @SerializedName("Offset")
    private float[] offset = new float[]{0, 0, 0};

    @SerializedName("Rotation")
    private float[] rotation = new float[]{0, 0, 0};

    public float[] scale() {
        return (this.scale != null && this.scale.length < 3) ? new float[]{1, 1, 1} : this.scale;
    }

    public float[] offset() {
        return (this.offset != null && this.offset.length < 3) ? new float[]{0, 0, 0} : this.offset;
    }

    public float[] rotation() {
        return (this.rotation != null && this.rotation.length < 3) ? new float[]{0, 0, 0} : this.rotation;
    }

    @SerializedName("XLength")
    public float xLength = 0.1f;

    @SerializedName("ZLength")
    public float zLength = 0.35f;

    @SerializedName("TickCount")
    public int tickCount = -1;

    /**
     * 无人机显示的挂载实体的实体数据
     */
    @SerializedName("DisplayData")
    public JsonObject displayData;
}
