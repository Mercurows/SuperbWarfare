package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class PotionMortarShellRecipe extends CustomRecipe {

    public PotionMortarShellRecipe(CraftingBookCategory pCategory) {
        super(pCategory);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level pLevel) {
        if (input.width() == 3 && input.height() == 3) {
            for (int i = 0; i < input.width(); ++i) {
                for (int j = 0; j < input.height(); ++j) {
                    int index = i + j * input.width();

                    ItemStack itemstack = input.getItem(index);

                    if (index % 2 == 0) {
                        if (i == 1 && j == 1) {
                            if (!itemstack.is(Items.LINGERING_POTION)) {
                                return false;
                            }
                        } else if (!itemstack.isEmpty()) {
                            return false;
                        }
                    } else if (!itemstack.is(ModItems.MORTAR_SHELL.get())) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack stack = input.getItem(1 + input.width());
        if (!stack.is(Items.LINGERING_POTION)) {
            return ItemStack.EMPTY;
        } else {
            ItemStack res = new ItemStack(ModItems.POTION_MORTAR_SHELL.get(), 4);
            res.set(DataComponents.POTION_CONTENTS, stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY));

            return res;
        }
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth >= 2 && pHeight >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.POTION_MORTAR_SHELL_SERIALIZER.get();
    }
}
