package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class JavelinMissileModel extends GeoModel<JavelinMissileEntity> {

    @Override
    public ResourceLocation getAnimationResource(JavelinMissileEntity entity) {
        return ModUtils.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(JavelinMissileEntity entity) {
        return ModUtils.loc("geo/javelin_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JavelinMissileEntity entity) {
        return ModUtils.loc("textures/entity/javelin_missile.png");
    }
}
