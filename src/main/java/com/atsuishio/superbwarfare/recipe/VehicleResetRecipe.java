package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class VehicleResetRecipe extends CustomRecipe {

    public VehicleResetRecipe(CraftingBookCategory pCategory) {
        super(pCategory);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level pLevel) {
        ItemStack kit = ItemStack.EMPTY;
        ItemStack container = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); ++i) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.VEHICLE_RESET_KIT.get())) {
                    if (!kit.isEmpty()) {
                        return false;
                    }
                    kit = stack;
                } else if (stack.is(ModItems.CONTAINER.get())) {
                    if (!container.isEmpty()) {
                        return false;
                    }
                    container = stack;
                }
            }
        }
        return !kit.isEmpty() && !container.isEmpty();
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack kit = ItemStack.EMPTY;
        ItemStack container = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); ++i) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.VEHICLE_RESET_KIT.get())) {
                    if (!kit.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    kit = stack.copy();
                } else if (stack.is(ModItems.CONTAINER.get())) {
                    if (!container.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                    container = stack.copy();
                }
            }
        }

        if (!kit.isEmpty() && !container.isEmpty()) {
            var data = container.get(DataComponents.BLOCK_ENTITY_DATA);
            CompoundTag tag = data != null ? data.copyTag() : new CompoundTag();
            if (tag.contains("EntityType")) {
                var type = tag.getString("EntityType");
                var entityType = EntityType.byString(type).orElse(null);
                if (entityType != null) {
                    return ContainerBlockItem.createInstance(entityType);
                }
            }

        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return pWidth * pHeight >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.VEHICLE_RESET_SERIALIZER.get();
    }
}
