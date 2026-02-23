package com.atsuishio.superbwarfare.recipe.vehicle

import com.atsuishio.superbwarfare.data.DataLoader
import com.google.gson.JsonObject
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer

class VehicleAssemblingRecipeSerializer : RecipeSerializer<VehicleAssemblingRecipe> {
    override fun fromJson(pRecipeId: ResourceLocation, pSerializedRecipe: JsonObject): VehicleAssemblingRecipe {
        val data = DataLoader.GSON.fromJson(
            pSerializedRecipe,
            VehicleAssemblingRecipeData::class.java
        )
        return VehicleAssemblingRecipe(pRecipeId, data)
    }

    override fun fromNetwork(pRecipeId: ResourceLocation, pBuffer: FriendlyByteBuf): VehicleAssemblingRecipe {
        val count = pBuffer.readVarInt()
        val ingredients = mutableListOf<VehicleAssemblingIngredient>()
        repeat(count) {
            val assemblingIngredient = VehicleAssemblingIngredient()
            assemblingIngredient.ingredientObject = Ingredient.fromNetwork(pBuffer)
            assemblingIngredient.count = pBuffer.readInt()
            ingredients.add(assemblingIngredient)
        }
        val category = pBuffer.readEnum(VehicleAssemblingRecipe.Category::class.java)
        val resultItem = pBuffer.readItem()
        val result = VehicleAssemblingResult()
        result.result = resultItem
        return VehicleAssemblingRecipe(pRecipeId, category, result, ingredients)
    }

    override fun toNetwork(pBuffer: FriendlyByteBuf, pRecipe: VehicleAssemblingRecipe) {
        pBuffer.writeVarInt(pRecipe.inputs.size)
        for (ingredient in pRecipe.inputs) {
            ingredient.ingredient.toNetwork(pBuffer)
            pBuffer.writeInt(ingredient.count)
        }
        pBuffer.writeEnum(pRecipe.category)
        pBuffer.writeItem(pRecipe.result.getResult())
    }
}
