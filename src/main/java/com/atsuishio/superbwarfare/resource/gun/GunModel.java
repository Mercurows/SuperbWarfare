package com.atsuishio.superbwarfare.resource.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class GunModel {

    @SerializedName("Animation")
    public ResourceLocation animation;

    @SerializedName("Model")
    public ResourceLocation model;

    @SerializedName("LODModel")
    public ResourceLocation lodModel;

    @SerializedName("Texture")
    public ResourceLocation texture;

    @SerializedName("LODTexture")
    public ResourceLocation lodTexture;
}
