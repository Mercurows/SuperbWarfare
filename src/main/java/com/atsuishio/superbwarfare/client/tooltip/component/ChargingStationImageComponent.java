package com.atsuishio.superbwarfare.client.tooltip.component;

import net.minecraft.world.item.ItemStack;

public class ChargingStationImageComponent extends GunImageComponent {

    public int width;
    public int height;
    public ItemStack stack;

    public ChargingStationImageComponent(int width, int height, ItemStack stack) {
        super(width, height, stack);
    }

    public ChargingStationImageComponent(ItemStack stack) {
        this(32, 16, stack);
    }
}