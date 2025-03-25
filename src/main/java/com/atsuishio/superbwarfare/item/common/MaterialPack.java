package com.atsuishio.superbwarfare.item.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class MaterialPack extends Item {

    public MaterialPack(Rarity rarity) {
        super(new Properties().rarity(rarity));
    }
}
