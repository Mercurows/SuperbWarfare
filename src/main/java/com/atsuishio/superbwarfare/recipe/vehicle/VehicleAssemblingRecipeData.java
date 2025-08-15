package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.data.DataLoader;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VehicleAssemblingRecipeData {

    @SerializedName("Inputs")
    private ObjectToList<StringToObject<VehicleAssemblingIngredient>> inputs;

    @SerializedName("Result")
    private VehicleAssemblingResult result;

    @SerializedName("Category")
    private String category = "empty";

    @SuppressWarnings("unchecked")
    public List<VehicleAssemblingIngredient> getInputs() {
        return (List<VehicleAssemblingIngredient>) DataLoader.processValue(inputs);
    }

    public VehicleAssemblingResult getResult() {
        return result;
    }

    public String getCategory() {
        return category;
    }
}
