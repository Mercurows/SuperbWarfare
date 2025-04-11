package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public interface CustomRendererItem {
    Supplier<GeoItemRenderer<? extends Item>> getRenderer();
}
