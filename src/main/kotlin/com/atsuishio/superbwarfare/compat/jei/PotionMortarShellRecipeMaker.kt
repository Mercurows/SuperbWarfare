package com.atsuishio.superbwarfare.compat.jei

import com.atsuishio.superbwarfare.init.ModItems
import net.minecraft.core.Holder
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.crafting.*
import java.util.*

object PotionMortarShellRecipeMaker {
    fun createRecipes(): MutableList<RecipeHolder<CraftingRecipe>> {
        val group = "jei.potion_mortar_shell"
        val ingredient = Ingredient.of(ItemStack(ModItems.MORTAR_SHELL.get()))

        return BuiltInRegistries.POTION.stream().map { potion ->
            val input = ItemStack(Items.LINGERING_POTION)
            input.set<PotionContents?>(DataComponents.POTION_CONTENTS, PotionContents(Holder.direct(potion)))

            val output = ItemStack(ModItems.POTION_MORTAR_SHELL.get(), 4)
            output.set<PotionContents?>(DataComponents.POTION_CONTENTS, PotionContents(Holder.direct(potion)))

            val potionIngredient = Ingredient.of(input)
            val inputs = NonNullList.of(
                Ingredient.EMPTY,
                Ingredient.EMPTY, ingredient, Ingredient.EMPTY,
                ingredient, potionIngredient, ingredient,
                Ingredient.EMPTY, ingredient, Ingredient.EMPTY
            )

            val id = ResourceLocation.withDefaultNamespace(group + "." + output.descriptionId)
            RecipeHolder(
                id, ShapedRecipe(
                    group,
                    CraftingBookCategory.MISC,
                    ShapedRecipePattern(3, 3, inputs, Optional.empty()),
                    output
                ) as CraftingRecipe
            )
        }.toList()
    }
}
