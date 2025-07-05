package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.client.renderer.SimpleGunRenderer;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.Supplier;

public class GunRendererBuilder {

    public static <T extends GunItem & GeoAnimatable> Supplier<CustomGunRenderer<T>> simple(
            Supplier<GeoModel<T>> model,
            double x,
            double y,
            double z,
            double size
    ) {
        return simple(model, x, y, z, size, false);
    }

    public static <T extends GunItem & GeoAnimatable> Supplier<CustomGunRenderer<T>> simple(
            Supplier<GeoModel<T>> model,
            double x,
            double y,
            double z,
            double size,
            boolean useOldHandRender
    ) {
        return () -> new SimpleGunRenderer<>(model.get(), x, y, z, size, useOldHandRender);
    }
}
