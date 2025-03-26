package com.atsuishio.superbwarfare.client.tooltip.component;

import net.minecraft.world.item.ItemStack;

public class SecondaryCataclysmImageComponent extends GunImageComponent {

    public int width;
    public int height;
    public ItemStack stack;

    public SecondaryCataclysmImageComponent(int width, int height, ItemStack stack) {
        super(width, height, stack);
    }

    public SecondaryCataclysmImageComponent(ItemStack stack) {
        this(32, 16, stack);
    }

}