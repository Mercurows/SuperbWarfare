package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.data.DataLoader;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VehicleAssemblingRecipeSerializer implements RecipeSerializer<VehicleAssemblingRecipe> {

    @Override
    public VehicleAssemblingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        VehicleAssemblingRecipeData data = DataLoader.GSON.fromJson(pSerializedRecipe, VehicleAssemblingRecipeData.class);
        return new VehicleAssemblingRecipe(pRecipeId, data);
    }

    @Override
    public @Nullable VehicleAssemblingRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        int count = pBuffer.readVarInt();
        List<VehicleAssemblingIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ingredients.add(new VehicleAssemblingIngredient(Ingredient.fromNetwork(pBuffer), pBuffer.readInt()));
        }
        var category = pBuffer.readEnum(VehicleAssemblingRecipe.Category.class);
        var result = pBuffer.readItem();
        return new VehicleAssemblingRecipe(pRecipeId, category, new VehicleAssemblingResult(result), ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, VehicleAssemblingRecipe pRecipe) {
        pBuffer.writeVarInt(pRecipe.getInputs().size());
        for (var ingredient : pRecipe.getInputs()) {
            ingredient.ingredient().toNetwork(pBuffer);
            pBuffer.writeInt(ingredient.count());
        }
        pBuffer.writeEnum(pRecipe.getCategory());
        pBuffer.writeItem(pRecipe.getResult().getResult());
    }
}
