package com.atsuishio.superbwarfare.compat.jei;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class GunPerksCategory implements IRecipeCategory<ItemStack> {

    public static final ResourceLocation TEXTURE = Mod.loc("textures/gui/jei_gun_perks.png");
    public static final RecipeType<ItemStack> TYPE = RecipeType.create(Mod.MODID, "gun_perks", ItemStack.class);

    private final IDrawable background;
    private final IDrawable icon;

    public GunPerksCategory(IGuiHelper helper) {
        this.background = helper.drawableBuilder(TEXTURE, 0, 0, 144, 128)
                .setTextureSize(144, 128)
                .build();
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.AP_BULLET.get()));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void draw(ItemStack recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var name = recipe.getHoverName();
        guiGraphics.drawString(Minecraft.getInstance().font, name,
                80 - Minecraft.getInstance().font.width(name) / 2, 5, 5592405, false);
    }

    @SuppressWarnings("removal")
    @Override
    public @Nullable IDrawable getBackground() {
        return this.background;
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
        return 144;
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

        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(stack);

        for (int i = 0; i < sortedPerks.size(); i++) {
            var perkItem = sortedPerks.get(i).getItem().get();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 8) * 18, 21 + i / 8 * 18).addItemStack(perkItem.getDefaultInstance());
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
