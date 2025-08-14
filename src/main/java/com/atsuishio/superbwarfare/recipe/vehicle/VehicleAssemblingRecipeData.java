package com.atsuishio.superbwarfare.recipe.vehicle;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VehicleAssemblingRecipeData {

    @SerializedName("inputs")
    private List<VehicleAssemblingIngredient> inputs;

    @SerializedName("result")
    private VehicleAssemblingResult result;

    @SerializedName("category")
    private String category = "empty";

    public List<VehicleAssemblingIngredient> getInputs() {
        return inputs;
    }

    public VehicleAssemblingResult getResult() {
        return result;
    }

    public String getCategory() {
        return category;
    }
}
