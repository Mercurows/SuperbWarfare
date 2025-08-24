package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import org.jetbrains.annotations.NotNull;

public class NetheriteHammer extends Hammer {

    public NetheriteHammer() {
        super(Tiers.NETHERITE, 13, -3.2f, new CustomDamageProperty(false).fireResistant());
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        return new ItemStack(this);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack stack) {
        return false;
    }
}
