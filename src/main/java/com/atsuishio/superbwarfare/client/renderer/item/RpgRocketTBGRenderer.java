package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.RpgRocketTBGModel;
import com.atsuishio.superbwarfare.item.common.ammo.RpgRocketTBG;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RpgRocketTBGRenderer extends GeoItemRenderer<RpgRocketTBG> {

    public RpgRocketTBGRenderer() {
        super(new RpgRocketTBGModel());
    }

}
