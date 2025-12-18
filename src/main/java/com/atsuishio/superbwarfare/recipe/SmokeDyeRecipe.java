package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.item.M18SmokeGrenade;
import com.google.common.collect.Lists;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmokeDyeRecipe extends CustomRecipe {

    public SmokeDyeRecipe(ResourceLocation pId, CraftingBookCategory pCategory) {
        super(pId, pCategory);
    }

    @Override
    public boolean matches(CraftingContainer pInv, @NotNull Level pLevel) {
        ItemStack itemstack = ItemStack.EMPTY;
        List<ItemStack> list = Lists.newArrayList();

        for (int i = 0; i < pInv.getContainerSize(); ++i) {
            ItemStack stack = pInv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.M18_SMOKE_GRENADE.get())) {
                    if (!itemstack.isEmpty()) {
                        return false;
                    }

                    itemstack = stack;
                } else {
                    if (!(stack.getItem() instanceof DyeItem)) {
                        return false;
                    }

                    list.add(stack);
                }
            }
        }
        return !itemstack.isEmpty() && !list.isEmpty();
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer pContainer, @NotNull RegistryAccess pRegistryAccess) {
        List<DyeItem> list = Lists.newArrayList();
        ItemStack itemstack = ItemStack.EMPTY;

        for (int i = 0; i < pContainer.getContainerSize(); ++i) {
            ItemStack stack = pContainer.getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                if (stack.is(ModItems.M18_SMOKE_GRENADE.get())) {
                    if (!itemstack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    itemstack = stack.copy();
                } else {
                    if (!(item instanceof DyeItem dyeItem)) {
                        return ItemStack.EMPTY;
                    }
                    list.add(dyeItem);
                }
            }
        }

        return !itemstack.isEmpty() && !list.isEmpty() ? dyeItem(itemstack, list) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth * pHeight >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.SMOKE_DYE_SERIALIZER.get();
    }

    public static ItemStack dyeItem(ItemStack pStack, List<DyeItem> pDyes) {
        ItemStack itemstack;
        int[] colors = new int[3];
        int i = 0;
        int j = 0;
        if (pStack.getItem() instanceof M18SmokeGrenade grenade) {
            itemstack = pStack.copyWithCount(1);
            int color = grenade.getColor(pStack);
            if (color != 0xFFFFFF) {
                float r = (float) (color >> 16 & 255) / 255F;
                float g = (float) (color >> 8 & 255) / 255F;
                float b = (float) (color & 255) / 255F;
                i += (int) (Math.max(r, Math.max(g, b)) * 255F);
                colors[0] += (int) (r * 255F);
                colors[1] += (int) (g * 255F);
                colors[2] += (int) (b * 255F);
                ++j;
            }

            for (DyeItem dyeitem : pDyes) {
                float[] dyeColors = dyeitem.getDyeColor().getTextureDiffuseColors();
                int r = (int) (dyeColors[0] * 255F);
                int g = (int) (dyeColors[1] * 255F);
                int b = (int) (dyeColors[2] * 255F);
                i += Math.max(r, Math.max(g, b));
                colors[0] += r;
                colors[1] += g;
                colors[2] += b;
                ++j;
            }
        } else {
            return ItemStack.EMPTY;
        }

        int red = colors[0] / j;
        int green = colors[1] / j;
        int blue = colors[2] / j;
        float rate = (float) i / (float) j;
        float max = (float) Math.max(red, Math.max(green, blue));
        red = (int) ((float) red * rate / max);
        green = (int) ((float) green * rate / max);
        blue = (int) ((float) blue * rate / max);
        int color = (red << 8) + green;
        color = (color << 8) + blue;
        grenade.setColor(itemstack, color);
        return itemstack;
    }
}
