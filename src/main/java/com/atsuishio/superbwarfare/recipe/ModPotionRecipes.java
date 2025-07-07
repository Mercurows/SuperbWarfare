package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.init.ModPotions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPotionRecipes {

    @SubscribeEvent
    public static void register(FMLCommonSetupEvent event) {
        ItemStack water = potion(Potions.WATER);
        ItemStack shock = potion(ModPotions.SHOCK.get());
        ItemStack strongShock = potion(ModPotions.STRONG_SHOCK.get());
        ItemStack longShock = potion(ModPotions.LONG_SHOCK.get());
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(new PotionRecipe(Ingredient.of(water), Ingredient.of(Items.LIGHTNING_ROD), shock));
            BrewingRecipeRegistry.addRecipe(new PotionRecipe(Ingredient.of(shock), Ingredient.of(Items.GLOWSTONE_DUST), strongShock));
            BrewingRecipeRegistry.addRecipe(new PotionRecipe(Ingredient.of(shock), Ingredient.of(Items.REDSTONE), longShock));
        });
    }

    private static ItemStack potion(Potion potion) {
        return PotionUtils.setPotion(Items.POTION.getDefaultInstance(), potion);
    }

    public static class PotionRecipe extends BrewingRecipe {

        private final Ingredient input;
        private final Ingredient ingredient;

        public PotionRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
            super(input, ingredient, output);
            this.input = input;
            this.ingredient = ingredient;
        }

        @Override
        public boolean isInput(@NotNull ItemStack stack) {
            ItemStack[] matchingStacks = this.input.getItems();
            return matchingStacks.length == 0 ? stack.isEmpty() : Arrays.stream(matchingStacks).anyMatch((itemstack) -> ItemStack.isSameItemSameTags(itemstack, stack));
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            ItemStack[] matchingStacks = this.ingredient.getItems();
            return matchingStacks.length == 0 ? ingredient.isEmpty() : Arrays.stream(matchingStacks).anyMatch((itemstack) -> ItemStack.isSameItemSameTags(itemstack, ingredient));
        }
    }
}
