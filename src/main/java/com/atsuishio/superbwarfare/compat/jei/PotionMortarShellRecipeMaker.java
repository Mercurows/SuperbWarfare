package com.atsuishio.superbwarfare.compat.jei;

import com.atsuishio.superbwarfare.init.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;

import java.util.List;
import java.util.Optional;

public class PotionMortarShellRecipeMaker {

    public static List<RecipeHolder<CraftingRecipe>> createRecipes() {
        String group = "jei.potion_mortar_shell";
        Ingredient ingredient = Ingredient.of(new ItemStack(ModItems.MORTAR_SHELL.get()));

        return BuiltInRegistries.POTION.stream().map(potion -> {
            ItemStack input = new ItemStack(Items.LINGERING_POTION);
            input.set(DataComponents.POTION_CONTENTS, new PotionContents(Holder.direct(potion)));

            ItemStack output = new ItemStack(ModItems.POTION_MORTAR_SHELL.get(), 4);
            output.set(DataComponents.POTION_CONTENTS, new PotionContents(Holder.direct(potion)));

            Ingredient potionIngredient = Ingredient.of(input);
            NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY,
                    Ingredient.EMPTY, ingredient, Ingredient.EMPTY,
                    ingredient, potionIngredient, ingredient,
                    Ingredient.EMPTY, ingredient, Ingredient.EMPTY
            );

            ResourceLocation id = ResourceLocation.withDefaultNamespace(group + "." + output.getDescriptionId());
            return new RecipeHolder<>(id, (CraftingRecipe) new ShapedRecipe(group,
                    CraftingBookCategory.MISC,
                    new ShapedRecipePattern(3, 3, inputs, Optional.empty()),
                    output
            ));
        }).toList();
    }
}
