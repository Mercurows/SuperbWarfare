package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModPotions;
import com.momosoftworks.coldsweat.util.item.PotionUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@EventBusSubscriber(modid = Mod.MODID)
public class ModPotionRecipes {

    @SubscribeEvent
    public static void register(RegisterBrewingRecipesEvent event) {
        ItemStack water = potion(Potions.WATER);
        ItemStack shock = potion(ModPotions.SHOCK);
        ItemStack strongShock = potion(ModPotions.STRONG_SHOCK);
        ItemStack longShock = potion(ModPotions.LONG_SHOCK);

        event.getBuilder().addRecipe(createRecipe(Ingredient.of(water), Ingredient.of(Items.LIGHTNING_ROD), shock));
        event.getBuilder().addRecipe(createRecipe(Ingredient.of(shock), Ingredient.of(Items.GLOWSTONE_DUST), strongShock));
        event.getBuilder().addRecipe(createRecipe(Ingredient.of(shock), Ingredient.of(Items.REDSTONE), longShock));
    }

    private static ItemStack potion(Holder<Potion> potion) {
        return PotionUtils.setPotion(Items.POTION.getDefaultInstance(), potion);
    }

    private static IBrewingRecipe createRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
        return new BrewingRecipe(input, ingredient, output) {
            @Override
            public boolean isInput(@NotNull ItemStack stack) {
                ItemStack[] matchingStacks = input.getItems();
                return matchingStacks.length == 0 ? stack.isEmpty() : Arrays.stream(matchingStacks).anyMatch((itemstack) -> ItemStack.isSameItemSameComponents(itemstack, stack));
            }

            @Override
            public boolean isIngredient(@NotNull ItemStack stack) {
                ItemStack[] matchingStacks = ingredient.getItems();
                return matchingStacks.length == 0 ? stack.isEmpty() : Arrays.stream(matchingStacks).anyMatch((itemstack) -> ItemStack.isSameItemSameComponents(itemstack, stack));
            }
        };
    }

}
