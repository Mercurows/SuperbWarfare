package com.atsuishio.superbwarfare.data.drone_attachment;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class DroneAttachmentData implements IDBasedData<DroneAttachmentData> {
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

    @SerializedName("Data")
    public JsonObject data;

    /**
     * 无人机显示的挂载实体的实体数据
     */
    @SerializedName("DisplayData")
    private JsonObject displayData;

    /**
     * 无人机投弹实体的实体数据
     */
    @SerializedName("DropData")
    private JsonObject dropData;

    public JsonObject displayData() {
        if (this.data != null) {
            return this.data;
        }
        return this.displayData != null ? this.displayData : this.dropData;
    }

    public JsonObject dropData() {
        if (this.data != null) {
            return this.data;
        }
        return this.dropData != null ? this.dropData : this.displayData;
    }

    @Override
    public @NotNull String getId() {
        return this.itemID;
    }

    @Override
    public void setId(@NotNull String id) {
        this.itemID = id;
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
