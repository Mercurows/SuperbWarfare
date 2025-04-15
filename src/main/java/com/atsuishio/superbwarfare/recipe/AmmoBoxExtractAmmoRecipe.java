package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBox;
import com.atsuishio.superbwarfare.tools.Ammo;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class AmmoBoxExtractAmmoRecipe extends CustomRecipe {

    public AmmoBoxExtractAmmoRecipe(CraftingBookCategory pCategory) {
        super(pCategory);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean matches(CraftingInput input, Level level) {
        var hasAmmoBox = false;
        var ammoBoxItem = ItemStack.EMPTY;

        for (var item : input.items()) {
            if (item.getItem() instanceof AmmoBox) {
                if (hasAmmoBox) return false;
                hasAmmoBox = true;
                ammoBoxItem = item;
            } else if (!item.isEmpty()) {
                return false;
            }
        }

        var data = ammoBoxItem.get(ModDataComponents.AMMO_BOX_INFO);
        if (data == null) return false;

        var typeString = data.type();
        var type = Ammo.getType(typeString);
        if (type == null) return false;

        return type.get(ammoBoxItem) > 0;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Ammo type = null;

        for (var item : input.items()) {
            if (item.getItem() instanceof AmmoBox) {
                var data = item.get(ModDataComponents.AMMO_BOX_INFO);
                assert data != null;
                type = Ammo.getType(data.type());
                break;
            }
        }

        assert type != null;

        // 也许这边有更好的方案？
        return switch (type) {
            case HANDGUN -> new ItemStack(ModItems.HANDGUN_AMMO.get());
            case RIFLE -> new ItemStack(ModItems.RIFLE_AMMO.get());
            case SHOTGUN -> new ItemStack(ModItems.SHOTGUN_AMMO.get());
            case SNIPER -> new ItemStack(ModItems.SNIPER_AMMO.get());
            case HEAVY -> new ItemStack(ModItems.HEAVY_AMMO.get());
        };
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        var remaining = super.getRemainingItems(input);

        for (int i = 0; i < input.items().size(); i++) {
            var item = input.getItem(i);
            if (item.getItem() instanceof AmmoBox) {
                var ammoBox = item.copy();

                var data = ammoBox.get(ModDataComponents.AMMO_BOX_INFO);
                assert data != null;
                Ammo type = Ammo.getType(data.type());

                assert type != null;
                type.add(ammoBox, -1);
                remaining.set(i, ammoBox);

                break;
            }
        }

        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.AMMO_BOX_ADD_AMMO_SERIALIZER.get();
    }
}
