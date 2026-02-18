package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.model.item.MilitaryShovelModel;
import com.atsuishio.superbwarfare.item.MilitaryShovel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MilitaryShovelRenderer extends GeoItemRenderer<MilitaryShovel> {

    public MilitaryShovelRenderer() {
        super(new MilitaryShovelModel());
    }

}
