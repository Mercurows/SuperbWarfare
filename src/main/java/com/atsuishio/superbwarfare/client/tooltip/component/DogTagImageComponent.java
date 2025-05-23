package com.atsuishio.superbwarfare.client.tooltip.component;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class DogTagImageComponent implements TooltipComponent {

    public int width;
    public int height;
    public ItemStack stack;

    public DogTagImageComponent(int width, int height, ItemStack stack) {
        this.width = width;
        this.height = height;
        this.stack = stack;
    }

    public DogTagImageComponent(ItemStack stack) {
        this(80, 80, stack);
    }
}
