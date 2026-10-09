package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.model.item.BocekItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.item.gun.special.BocekItem;

public class BocekItemRenderer extends CustomGunRenderer<BocekItem> {
    public BocekItemRenderer() {
        super(new BocekItemModel());
    }
}
