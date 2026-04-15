package com.atsuishio.superbwarfare.resource.vehicle;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.resource.ModelResource;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

public class DefaultVehicleResource implements IDBasedData<DefaultVehicleResource> {

    private transient String id = "";

    @Override
    public @NotNull String getId() {
        return this.id;
    }

    @Override
    public void setId(@NotNull String id) {
        this.id = id;
    }

    @SerializedName("Model")
    private ModelResource model = new ModelResource();

    public ModelResource getModel() {
        return model == null ? new ModelResource() : model;
    }

    @SerializedName("LODDistance")
    public ObjectToList<Double> lodDistance = new ObjectToList<>(48.0, 96.0);
}
