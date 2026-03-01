package com.atsuishio.superbwarfare.init

import net.minecraft.ChatFormatting
import net.minecraft.world.item.Rarity

object ModRarities {
    @JvmField
    val LEGENDARY: Rarity = Rarity.create("superbwarfare_legendary", ChatFormatting.GOLD)

    @JvmField
    val SUPERB: Rarity = Rarity.create("superbwarfare_superb", ChatFormatting.RED)

    @JvmField
    val VIRTUAL: Rarity = Rarity.create("superbwarfare_virtual") { it.withColor(0xFF9AAF) }
}