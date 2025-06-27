package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.Tm62ItemModel;
import com.atsuishio.superbwarfare.item.Tm62Item;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class Tm62ItemRenderer extends GeoItemRenderer<Tm62Item> {

    public Tm62ItemRenderer() {
        super(new Tm62ItemModel());
    }

    @Override
    public ResourceLocation getTextureLocation(Tm62Item instance) {
        return super.getTextureLocation(instance);
    }
}
