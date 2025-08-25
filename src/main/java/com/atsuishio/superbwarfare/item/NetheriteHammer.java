package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;

public class NetheriteHammer extends Hammer {

    public NetheriteHammer() {
        super(Tiers.NETHERITE, 13, -3.2f, new Item.Properties().durability(2800).fireResistant());
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }
}
