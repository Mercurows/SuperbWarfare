package com.atsuishio.superbwarfare.init;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

public class ModRarity {
    public static final EnumProxy<Rarity> SUPERBWARFARE_LEGENDARY = new EnumProxy<>(
            Rarity.class,
            -1,
            "superbwarfare:legendary",
            ChatFormatting.GOLD
    );

    public static Rarity getLegendary() {
        return SUPERBWARFARE_LEGENDARY.getValue();
    }
}
