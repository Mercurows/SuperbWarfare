package com.atsuishio.superbwarfare.client.tooltip.component;

import net.minecraft.world.item.ItemStack;

public class BocekImageComponent extends GunImageComponent {

    public int width;
    public int height;
    public ItemStack stack;

    public BocekImageComponent(int width, int height, ItemStack stack) {
        super(width, height, stack);
    }

    public BocekImageComponent(ItemStack stack) {
        this(32, 16, stack);
    }

}