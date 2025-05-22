package com.atsuishio.superbwarfare.client.tooltip.component;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class CellImageComponent implements TooltipComponent {

    public int width;
    public int height;
    public ItemStack stack;

    public CellImageComponent(int width, int height, ItemStack stack) {
        this.width = width;
        this.height = height;
        this.stack = stack;
    }

    public CellImageComponent(ItemStack stack) {
        this(32, 16, stack);
    }
}