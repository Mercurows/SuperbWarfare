package com.atsuishio.superbwarfare.resource;

import com.atsuishio.superbwarfare.data.ObjectToList;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class ModelResource {

    @SerializedName("Animation")
    public ResourceLocation animation;

    @SerializedName("Model")
    public ResourceLocation model;

    @SerializedName("LODModel")
    protected ObjectToList<ResourceLocation> lodModel = new ObjectToList<>();

    public boolean hasLOD() {
        return lodModel != null && !lodModel.list.isEmpty();
    }

    // LOD的最小等级为1
    public ResourceLocation getLODModel(int level) {
        if (level < 1 || lodModel == null || lodModel.list.isEmpty()) return model;

        var availableLevel = Math.min(level - 1, lodModel.list.size() - 1);
        return lodModel.list.get(availableLevel);
    }

    @SerializedName("Texture")
    public ResourceLocation texture;

    @SerializedName("LODTexture")
    protected ObjectToList<ResourceLocation> lodTexture;

    // LOD的最小等级为1
    public ResourceLocation getLODTexture(int level) {
        if (level < 1 || lodTexture == null || lodTexture.list.isEmpty()) return texture;

        var availableLevel = Math.min(level - 1, lodTexture.list.size() - 1);
        return lodTexture.list.get(availableLevel);
    }
}
