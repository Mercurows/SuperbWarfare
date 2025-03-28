package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WheelChairModel extends GeoModel<WheelChairEntity> {

    @Override
    public ResourceLocation getAnimationResource(WheelChairEntity entity) {
        return null;
//        return ModUtils.loc("animations/wheel_chair.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(WheelChairEntity entity) {
        return Mod.loc("geo/wheel_chair.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WheelChairEntity entity) {
        return Mod.loc("textures/entity/wheel_chair.png");
    }
}
