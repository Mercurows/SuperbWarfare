package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.init.ModMenuTypes;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VehicleAssemblingMenu extends AbstractContainerMenu {

    public VehicleAssemblingMenu(int pContainerId, Inventory inventory) {
        super(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), pContainerId);

    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.isAlive();
    }

    public void assembleVehicle(ResourceLocation id, Player player) {
        var recipe = this.getRecipeById(id, player.level().getRecipeManager());
        if (recipe == null) return;
        player.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {

        });
    }

    @Nullable
    public VehicleAssemblingRecipe getRecipeById(ResourceLocation id, RecipeManager recipeManager) {
        Recipe<?> recipe = recipeManager.byKey(id).orElse(null);
        if (recipe instanceof VehicleAssemblingRecipe assemblingRecipe) {
            return assemblingRecipe;
        }
        return null;
    }
}
