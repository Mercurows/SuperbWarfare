package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.RocketItemModel;
import com.atsuishio.superbwarfare.item.common.ammo.RocketItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RocketItemRenderer extends GeoItemRenderer<RocketItem> {

    public RocketItemRenderer() {
        super(new RocketItemModel());
    }

    @Override
    public ResourceLocation getTextureLocation(RocketItem instance) {
        return super.getTextureLocation(instance);
    }
}
