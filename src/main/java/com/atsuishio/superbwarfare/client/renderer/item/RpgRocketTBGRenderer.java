package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.RpgRocketTBGModel;
import com.atsuishio.superbwarfare.item.common.ammo.RpgRocketTBG;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RpgRocketTBGRenderer extends GeoItemRenderer<RpgRocketTBG> {

    public RpgRocketTBGRenderer() {
        super(new RpgRocketTBGModel());
    }

    @Override
    public ResourceLocation getTextureLocation(RpgRocketTBG instance) {
        return super.getTextureLocation(instance);
    }
}
