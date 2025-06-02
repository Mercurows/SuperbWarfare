package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.Tm62ItemModel;
import com.atsuishio.superbwarfare.item.Tm62;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class Tm62ItemRenderer extends GeoItemRenderer<Tm62> {

    public Tm62ItemRenderer() {
        super(new Tm62ItemModel());
    }

    @Override
    public ResourceLocation getTextureLocation(Tm62 instance) {
        return super.getTextureLocation(instance);
    }
}
