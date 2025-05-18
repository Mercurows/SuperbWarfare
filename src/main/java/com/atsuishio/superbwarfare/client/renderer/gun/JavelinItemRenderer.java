package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.model.item.JavelinItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;

public class JavelinItemRenderer extends CustomGunRenderer<JavelinItem> {

    public JavelinItemRenderer() {
        super(new JavelinItemModel());
    }
}
