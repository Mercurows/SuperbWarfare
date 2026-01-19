package com.atsuishio.superbwarfare.init;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;

public class ModRarities {

    public static final Rarity LEGENDARY = Rarity.create("superbwarfare_legendary", ChatFormatting.GOLD);
    public static final Rarity SUPERB = Rarity.create("superbwarfare_superb", ChatFormatting.RED);
    public static final Rarity VIRTUAL = Rarity.create("superbwarfare_virtual", s -> s.withColor(0xFF9AAF));
}
