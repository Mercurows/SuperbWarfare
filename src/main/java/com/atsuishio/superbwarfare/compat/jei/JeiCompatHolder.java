package com.atsuishio.superbwarfare.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public class JeiCompatHolder {

    public static final String JEI = "jei";

    public static boolean hasJEI() {
        return ModList.get().isLoaded(JEI);
    }

    public static boolean showRecipes(ItemStack stack) {
        return SbwJEIPlugin.showRecipes(stack);
    }
}
