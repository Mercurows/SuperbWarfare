package com.atsuishio.superbwarfare.resource.vehicle;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.resource.ModelResource;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec2;

public class DefaultVehicleResource implements IDBasedData<DefaultVehicleResource> {

    @SerializedName("ID")
    public String id = "";

    @Override
    public String getId() {
        return this.id;
    }

    @SerializedName("Model")
    private ModelResource model = new ModelResource();

    public ModelResource getModel() {
        return model == null ? new ModelResource() : model;
    }

    @SerializedName("LODDistance")
    public ObjectToList<Double> lodDistance = new ObjectToList<>(48.0, 96.0);

    @SerializedName("MouseSpeed")
    public Vec2 mouseSpeed = new Vec2(0.4f, 0.4f);
}
