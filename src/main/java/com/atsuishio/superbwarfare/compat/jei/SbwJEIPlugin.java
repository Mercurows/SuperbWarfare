package com.atsuishio.superbwarfare.compat.jei;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.NBTTool;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
public class SbwJEIPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return Mod.loc("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GunPerksCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.REFORGING_TABLE.get()), GunPerksCategory.TYPE);
    }

    // TODO 正确注册subtypes
    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        var guns = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof GunItem).map(Item::getDefaultInstance).toList();
        registration.addRecipes(GunPerksCategory.TYPE, guns);

        registration.addItemStackInfo(new ItemStack(ModItems.ANCIENT_CPU.get()), Component.translatable("jei.superbwarfare.ancient_cpu"));
        registration.addItemStackInfo(new ItemStack(ModItems.CHARGING_STATION.get()), Component.translatable("jei.superbwarfare.charging_station"));

        var specialCraftingRecipes = PotionMortarShellRecipeMaker.createRecipes();
        registration.addRecipes(RecipeTypes.CRAFTING, specialCraftingRecipes);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.CONTAINER.get(), new ISubtypeInterpreter<>() {
            @Override
            public @NotNull Object getSubtypeData(ItemStack ingredient, @NotNull UidContext context) {
                var data = ingredient.get(DataComponents.BLOCK_ENTITY_DATA);
                var tag = data != null ? data.copyTag() : new CompoundTag();
                if (tag.contains("EntityType")) {
                    return tag.getString("EntityType");
                }
                return "";
            }

            @Override
            @ParametersAreNonnullByDefault
            public @NotNull String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
                return (String) getSubtypeData(ingredient, context);
            }
        });

        registration.registerSubtypeInterpreter(ModItems.POTION_MORTAR_SHELL.get(), new ISubtypeInterpreter<>() {
            @Override
            @ParametersAreNonnullByDefault
            public @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
                PotionContents contents = ingredient.get(DataComponents.POTION_CONTENTS);
                if (contents == null) {
                    return null;
                }
                return contents.potion().orElse(null);
            }

            @Override
            public @NotNull String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
                if (ingredient.getComponentsPatch().isEmpty()) {
                    return "";
                }
                PotionContents contents = ingredient.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                String itemDescriptionId = ingredient.getItem().getDescriptionId();
                String potionEffectId = contents.potion().map(Holder::getRegisteredName).orElse("none");
                return itemDescriptionId + ".effect_id." + potionEffectId;
            }
        });

        registration.registerSubtypeInterpreter(ModItems.C4_BOMB.get(), new ISubtypeInterpreter<>() {
            @Override
            public @NotNull Object getSubtypeData(ItemStack ingredient, UidContext context) {
                return NBTTool.getTag(ingredient).getBoolean("Control");
            }

            @Override
            public @NotNull String getLegacyStringSubtypeInfo(ItemStack ingredient, UidContext context) {
                return (String) getSubtypeData(ingredient, context);
            }
        });
    }
}
