package com.atsuishio.superbwarfare.recipe;

import com.atsuishio.superbwarfare.data.gun.Ammo;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModRecipes;
import com.atsuishio.superbwarfare.item.common.ammo.AmmoBox;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AmmoBoxExtractAmmoRecipe extends CustomRecipe {

    public AmmoBoxExtractAmmoRecipe(ResourceLocation pId, CraftingBookCategory pCategory) {
        super(pId, pCategory);
    }

    @Override
    public boolean matches(CraftingContainer pContainer, @NotNull Level pLevel) {
        var hasAmmoBox = false;
        var ammoBoxItem = ItemStack.EMPTY;

        for (var item : pContainer.getItems()) {
            if (item.getItem() instanceof AmmoBox) {
                if (hasAmmoBox) return false;
                hasAmmoBox = true;
                ammoBoxItem = item;
            } else if (!item.isEmpty()) {
                return false;
            }
        }

        var tag = ammoBoxItem.getTag();
        if (tag == null) return false;

        var typeString = tag.getString("Type");
        var type = Ammo.getType(typeString);
        if (type == null) return false;

        return type.get(ammoBoxItem) > 0;
    }


    @Override
    public @NotNull ItemStack assemble(CraftingContainer pContainer, @NotNull RegistryAccess pRegistryAccess) {
        Ammo type = null;

        for (var item : pContainer.getItems()) {
            if (item.getItem() instanceof AmmoBox) {
                type = Ammo.getType(item.getOrCreateTag().getString("Type"));
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
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer pContainer) {
        var remaining = super.getRemainingItems(pContainer);

        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            var item = pContainer.getItem(i);
            if (item.getItem() instanceof AmmoBox) {
                var ammoBox = item.copy();
                Ammo type = Ammo.getType(item.getOrCreateTag().getString("Type"));

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
