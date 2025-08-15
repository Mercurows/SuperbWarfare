package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.google.gson.annotations.SerializedName;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.regex.Pattern;

public class VehicleAssemblingIngredient implements DeserializeFromString {
    @SerializedName("Ingredient")
    String ingredientString = "";
    @SerializedName("Count")
    int count = 1;

    public transient Ingredient ingredientObject;

    private static final Pattern INGREDIENT_PATTERN = Pattern.compile("^(?<count>(\\d+)?)\\s*(x\\s*)?(?<prefix>#?)(?<id>\\w+:\\w+)$");

    public Ingredient getIngredient() {
        if (ingredientObject == null) {
            deserializeFromString(ingredientString);
        }
        return ingredientObject;
    }

    @Override
    public void deserializeFromString(String str) {
        var matcher = INGREDIENT_PATTERN.matcher(str);
        if (!matcher.matches()) {
            Mod.LOGGER.warn("invalid vehicle assembling ingredient: {}", str);
            ingredientObject = Ingredient.EMPTY;
            return;
        }

        var countString = matcher.group("count");
        if (!countString.isEmpty()) {
            count = Math.max(1, Integer.parseInt(countString));
        }

        var id = matcher.group("id");
        if (matcher.group("prefix").equals("#")) {
            ingredientObject = Ingredient.of(TagKey.create(Registries.ITEM, new ResourceLocation(id)));
        } else {
            ingredientObject = Ingredient.of(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));
        }
    }
}
