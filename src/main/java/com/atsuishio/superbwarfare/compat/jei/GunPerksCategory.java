package com.atsuishio.superbwarfare.compat.jei;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// TODO 来个正常一点的背景
public class GunPerksCategory implements IRecipeCategory<ItemStack> {

    public static final RecipeType<ItemStack> TYPE = RecipeType.create(Mod.MODID, "gun_perks", ItemStack.class);

    private final IDrawable icon;

    public GunPerksCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.AP_BULLET.get()));
    }

    @Override
    public @NotNull RecipeType<ItemStack> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.superbwarfare.gun_perks");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 140;
    }

    @Override
    public int getHeight() {
        return 128;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, ItemStack stack, @NotNull IFocusGroup focuses) {
        if (!(stack.getItem() instanceof GunItem)) return;
        GunData data = GunData.from(stack);
        var perks = data.availablePerks();
        List<Perk> sortedPerks = new ArrayList<>(perks);
        sortedPerks.sort((a, b) -> {
            int aIndex = getIndex(a);
            int bIndex = getIndex(b);
            return (aIndex == bIndex) ? a.name.compareTo(b.name) : aIndex - bIndex;
        });

        builder.addSlot(RecipeIngredientRole.INPUT, 5, 0).addItemStack(stack);

        for (int i = 0; i < sortedPerks.size(); i++) {
            var perkItem = sortedPerks.get(i).getItem().get();
            builder.addSlot(RecipeIngredientRole.OUTPUT, 5 + (i % 7) * 18, 20 + i / 7 * 18).addItemStack(perkItem.getDefaultInstance());
        }
    }

    private static int getIndex(Perk perk) {
        return switch (perk.type) {
            case AMMO -> 0;
            case FUNCTIONAL -> 1;
            case DAMAGE -> 2;
        };
    }
}
