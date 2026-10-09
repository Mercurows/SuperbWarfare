package com.atsuishio.superbwarfare.client.renderer;


import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class SimpleGunRenderer<T extends GunGeoItem & GeoAnimatable> extends CustomGunRenderer<T> {
    public SimpleGunRenderer(GeoModel<T> model) {
        super(model);
    }
}
