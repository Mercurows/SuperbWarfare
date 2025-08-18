package com.atsuishio.superbwarfare.compat.jei;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class SbwJEIPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return Mod.loc("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GunPerksCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new VehicleAssemblingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.REFORGING_TABLE.get()), GunPerksCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get()), VehicleAssemblingCategory.TYPE);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        RecipeManager recipeManager = level.getRecipeManager();

        var guns = ForgeRegistries.ITEMS.getValues().stream().filter(item -> item instanceof GunItem).map(Item::getDefaultInstance).toList();
        registration.addRecipes(GunPerksCategory.TYPE, guns);
        registration.addRecipes(VehicleAssemblingCategory.TYPE, recipeManager.getAllRecipesFor(ModRecipes.VEHICLE_ASSEMBLING_TYPE.get()));

        registration.addItemStackInfo(new ItemStack(ModItems.ANCIENT_CPU.get()), Component.translatable("jei.superbwarfare.ancient_cpu"));
        registration.addItemStackInfo(new ItemStack(ModItems.CHARGING_STATION.get()), Component.translatable("jei.superbwarfare.charging_station"));

        List<CraftingRecipe> specialCraftingRecipes = PotionMortarShellRecipeMaker.createRecipes();
        registration.addRecipes(RecipeTypes.CRAFTING, specialCraftingRecipes);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.CONTAINER.get(), (ingredient, context) -> {
                    if (ingredient.getTag() == null) return IIngredientSubtypeInterpreter.NONE;
                    return ingredient.getTag().getCompound("BlockEntityTag").getString("EntityType");
                }
        );

        registration.registerSubtypeInterpreter(ModItems.POTION_MORTAR_SHELL.get(), (stack, context) -> {
            if (!stack.hasTag()) {
                return IIngredientSubtypeInterpreter.NONE;
            }
            Potion potionType = PotionUtils.getPotion(stack);
            String potionTypeString = potionType.getName("");
            StringBuilder stringBuilder = new StringBuilder(potionTypeString);
            List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
            for (MobEffectInstance effect : effects) {
                stringBuilder.append(";").append(effect);
            }

            return stringBuilder.toString();
        });

        registration.registerSubtypeInterpreter(ModItems.C4_BOMB.get(), ((ingredient, context) -> {
            if (ingredient.getTag() == null) return IIngredientSubtypeInterpreter.NONE;
            return String.valueOf(ingredient.getTag().getBoolean("Control"));
        }));
    }
}
