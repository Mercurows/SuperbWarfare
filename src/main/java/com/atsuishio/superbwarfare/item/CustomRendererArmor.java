package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public interface CustomRendererArmor {
    GeoArmorRenderer<? extends Item> getRenderer();
}
