package com.atsuishio.superbwarfare.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public interface ItemScreenProvider {

    @OnlyIn(Dist.CLIENT)
    @Nullable Screen getItemScreen(ItemStack stack, Player player);
}
