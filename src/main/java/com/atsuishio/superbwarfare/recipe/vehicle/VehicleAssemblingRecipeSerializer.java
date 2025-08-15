package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.data.DataLoader;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class VehicleAssemblingRecipeSerializer implements RecipeSerializer<VehicleAssemblingRecipe> {

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull VehicleAssemblingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pSerializedRecipe) {
        VehicleAssemblingRecipeData data = DataLoader.GSON.fromJson(pSerializedRecipe, VehicleAssemblingRecipeData.class);
        return new VehicleAssemblingRecipe(pRecipeId, data);
    }

    @Override
    public @Nullable VehicleAssemblingRecipe fromNetwork(@NotNull ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        int count = pBuffer.readVarInt();
        List<VehicleAssemblingIngredient> ingredients = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            var assemblingIngredient = new VehicleAssemblingIngredient();
            assemblingIngredient.ingredientObject = Ingredient.fromNetwork(pBuffer);
            assemblingIngredient.count = pBuffer.readInt();
            ingredients.add(assemblingIngredient);
        }
        var category = pBuffer.readEnum(VehicleAssemblingRecipe.Category.class);
        var resultItem = pBuffer.readItem();
        var result = new VehicleAssemblingResult();
        result.result = resultItem;
        return new VehicleAssemblingRecipe(pRecipeId, category, result, ingredients);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, VehicleAssemblingRecipe pRecipe) {
        pBuffer.writeVarInt(pRecipe.getInputs().size());
        for (var ingredient : pRecipe.getInputs()) {
            ingredient.getIngredient().toNetwork(pBuffer);
            pBuffer.writeInt(ingredient.count);
        }
        pBuffer.writeEnum(pRecipe.getCategory());
        pBuffer.writeItem(pRecipe.getResult().getResult());
    }
}
