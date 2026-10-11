package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.model.item.TracheliumItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.item.gun.handgun.TracheliumItem;

public class TracheliumItemRenderer extends CustomGunRenderer<TracheliumItem> {
    public TracheliumItemRenderer() {
        super(new TracheliumItemModel());
    }
}