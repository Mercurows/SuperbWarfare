package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.tools.OBB;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class OBBInfo {

    @SerializedName("Size")
    public Vec3 size;

    @SerializedName("Position")
    public Vec3 position;

    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Rotation")
    public String rotation = "Default";

    @SerializedName("Part")
    public OBB.Part part = OBB.Part.BODY;

    private transient OBB obb;

    public OBB getOBB() {
        if (this.obb == null) {
            this.obb = new OBB(Vec3.ZERO.toVector3f(), this.size.toVector3f(), new Quaternionf(), this.part);
        }
        return this.obb;
    }

    public void limit() {
        if (this.size == null) this.size = Vec3.ZERO;
        if (this.position == null) this.position = Vec3.ZERO;
        if (this.transform == null || this.transform.isBlank()) this.transform = "Vehicle";
        if (this.part == null) this.part = OBB.Part.BODY;
    }
}
