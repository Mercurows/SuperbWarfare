package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.JavelinMissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class JavelinMissileModel extends GeoModel<JavelinMissileEntity> {

    @Override
    public ResourceLocation getAnimationResource(JavelinMissileEntity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(JavelinMissileEntity entity) {
        return Mod.loc("geo/javelin_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JavelinMissileEntity entity) {
        return Mod.loc("textures/entity/javelin_missile.png");
    }
}
