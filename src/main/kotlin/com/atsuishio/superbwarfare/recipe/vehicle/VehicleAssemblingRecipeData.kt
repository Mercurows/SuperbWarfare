package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.data.DataLoader.processValue
import com.atsuishio.superbwarfare.data.ObjectToList
import com.atsuishio.superbwarfare.data.StringToObject
import com.google.gson.annotations.SerializedName

class VehicleAssemblingRecipeData {
    @SerializedName("inputs")
    @get:JvmName("inputs")
    val inputs: ObjectToList<StringToObject<VehicleAssemblingIngredient>>? = null

    @SerializedName("result")
    val result: VehicleAssemblingResult? = null

    @SerializedName("category")
    val category: String = "empty"

    @Suppress("UNCHECKED_CAST")
    fun getInputs(): MutableList<VehicleAssemblingIngredient>? {
        return processValue(inputs) as MutableList<VehicleAssemblingIngredient>?
    }
}
