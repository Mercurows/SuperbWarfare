package com.atsuishio.superbwarfare.compat.jei;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class VehicleAssemblingCategory implements IRecipeCategory<VehicleAssemblingRecipe> {

    public static final ResourceLocation TEXTURE = Mod.loc("textures/gui/jei_vehicle_assembling_table.png");
    public static final RecipeType<VehicleAssemblingRecipe> TYPE = new RecipeType<>(Mod.loc("vehicle_assembling"), VehicleAssemblingRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public VehicleAssemblingCategory(IGuiHelper helper) {
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, 144, 36)
                .setTextureSize(144, 36)
                .build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get()));
    }

    @SuppressWarnings("removal")
    @Override
    public @Nullable IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @NotNull RecipeType<VehicleAssemblingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.superbwarfare.vehicle_assembling");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 144;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VehicleAssemblingRecipe recipe, @NotNull IFocusGroup focuses) {
        var res = recipe.getResult();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 1, 1).addItemStack(res.getResult().copyWithCount(res.count));

        for (int i = 0; i < recipe.getInputs().size(); i++) {
            if (i >= 12) return;
            var ingredient = recipe.getInputs().get(i).getIngredient().getItems();
            int finalI = i;
            Arrays.stream(ingredient).forEach((stack) -> stack.setCount(recipe.getInputs().get(finalI).getCount()));
            builder.addSlot(RecipeIngredientRole.INPUT, 37 + (i % 6) * 18, 1 + i / 6 * 18).addItemStacks(List.of(ingredient));
        }
    }
}
