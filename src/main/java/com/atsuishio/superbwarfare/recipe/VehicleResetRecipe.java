package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class VehicleResetRecipe extends CustomRecipe {

    public VehicleResetRecipe(ResourceLocation pId, CraftingBookCategory pCategory) {
        super(pId, pCategory);
    }

    @Override
    public boolean matches(CraftingContainer pContainer, Level pLevel) {
        ItemStack kit = ItemStack.EMPTY;
        ItemStack container = ItemStack.EMPTY;

        for (int i = 0; i < pContainer.getContainerSize(); ++i) {
            ItemStack stack = pContainer.getItem(i);
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
    public ItemStack assemble(CraftingContainer pContainer, RegistryAccess pRegistryAccess) {
        ItemStack kit = ItemStack.EMPTY;
        ItemStack container = ItemStack.EMPTY;

        for (int i = 0; i < pContainer.getContainerSize(); ++i) {
            ItemStack stack = pContainer.getItem(i);
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
            var tag = BlockItem.getBlockEntityData(container);
            if (tag != null) {
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.VEHICLE_RESET_SERIALIZER.get();
    }
}
