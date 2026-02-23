package com.atsuishio.superbwarfare.compat.jei

import net.minecraft.world.item.ItemStack
import net.minecraftforge.fml.ModList

object JeiCompatHolder {
    const val JEI: String = "jei"

    @JvmStatic
    fun hasJEI(): Boolean {
        return ModList.get().isLoaded(JEI)
    }

    @JvmStatic
    fun showRecipes(stack: ItemStack): Boolean {
        return SbwJEIPlugin.showRecipes(stack)
    }
}
