package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.tiers.ModItemTier;
import net.minecraft.world.item.SwordItem;

public class ElectricBaton extends SwordItem implements EnergyStorageItem {
    public ElectricBaton() {
        super(ModItemTier.STEEL, new Properties()
                .durability(1114)
                .attributes(SwordItem.createAttributes(ModItemTier.STEEL, 3, -2.5f))
        );
    }

    @Override
    public int getMaxEnergy() {
        return 6000;
    }
}
