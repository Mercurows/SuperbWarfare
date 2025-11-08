package com.atsuishio.superbwarfare.resource.vehicle;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.resource.ModelResource;
import com.google.gson.annotations.SerializedName;

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

    @SerializedName("ZoomRate")
    public int zoomRate = 1;
}
