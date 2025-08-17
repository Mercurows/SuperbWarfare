package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.Ptkm1rItemModel;
import com.atsuishio.superbwarfare.item.Ptkm1rItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class Ptkm1rItemRenderer extends GeoItemRenderer<Ptkm1rItem> {

    public Ptkm1rItemRenderer() {
        super(new Ptkm1rItemModel());
    }

    @Override
    public ResourceLocation getTextureLocation(Ptkm1rItem instance) {
        return super.getTextureLocation(instance);
    }
}
