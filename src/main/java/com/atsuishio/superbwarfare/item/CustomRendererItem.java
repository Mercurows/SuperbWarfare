package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public interface CustomRendererItem {
    GeoItemRenderer<? extends Item> getRenderer();
}
