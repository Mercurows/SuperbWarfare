package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.RpgRocketStandardModel;
import com.atsuishio.superbwarfare.item.common.ammo.RpgRocketStandard;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RpgRocketStandardRenderer extends GeoItemRenderer<RpgRocketStandard> {

    public RpgRocketStandardRenderer() {
        super(new RpgRocketStandardModel());
    }

}
