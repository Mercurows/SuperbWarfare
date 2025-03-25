package com.atsuishio.superbwarfare.item.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class BlueprintItem extends Item {

    public BlueprintItem(Rarity rarity) {
        super(new Properties().rarity(rarity));
    }
}
