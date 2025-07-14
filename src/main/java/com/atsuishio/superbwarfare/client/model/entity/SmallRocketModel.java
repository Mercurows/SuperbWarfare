package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SmallRocketModel extends GeoModel<SmallRocketEntity> {

    @Override
    public ResourceLocation getAnimationResource(SmallRocketEntity entity) {
        return Mod.loc("animations/rpg_rocket.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SmallRocketEntity entity) {
        return Mod.loc("geo/small_rocket.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmallRocketEntity entity) {
        return Mod.loc("textures/entity/small_rocket.png");
    }
}
