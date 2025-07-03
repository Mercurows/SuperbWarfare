package com.atsuishio.superbwarfare.data.drone_attachment;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class DroneAttachmentData implements IDBasedData {
    @SerializedName("Item")
    public String itemID = "";

    @SerializedName("Entity")
    private String entity = "";
    @SerializedName("DisplayEntity")
    private String displayEntity = "";

    @SerializedName("DropEntity")
    private String dropEntity = "";

    public String displayEntity() {
        if (!this.entity.isEmpty()) {
            return this.entity;
        }
        return this.displayEntity.isEmpty() ? this.dropEntity : this.displayEntity;
    }

    public String dropEntity() {
        if (!this.entity.isEmpty()) {
            return this.entity;
        }
        return this.dropEntity.isEmpty() ? this.displayEntity : this.dropEntity;
    }


    @SerializedName("DropPosition")
    private float[] dropPosition = new float[]{0, -0.09f, 0};

    public float[] dropPosition() {
        return (this.dropPosition != null && this.dropPosition.length < 3) ? new float[]{0, -0.09f, 0} : this.dropPosition;
    }


    /**
     * 无人机显示的挂载实体的实体数据
     */
    @SerializedName("DisplayData")
    public JsonObject displayData;

    /**
     * 无人机投弹实体的实体数据
     */
    @SerializedName("DropData")
    public JsonObject dropData;

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
}
