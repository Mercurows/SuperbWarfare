package com.atsuishio.superbwarfare.client.tooltip.component;

import net.minecraft.world.item.ItemStack;

public class LauncherImageComponent extends GunImageComponent {

    public int width;
    public int height;
    public ItemStack stack;

    public LauncherImageComponent(int width, int height, ItemStack stack) {
        super(width, height, stack);
    }

    public LauncherImageComponent(ItemStack stack) {
        this(32, 16, stack);
    }

}