package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.HeliRocketEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HeliRocketModel extends GeoModel<HeliRocketEntity> {

    @Override
    public ResourceLocation getAnimationResource(HeliRocketEntity entity) {
        return Mod.loc("animations/rpg_rocket.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(HeliRocketEntity entity) {
        return Mod.loc("geo/heli_rocket.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HeliRocketEntity entity) {
        return Mod.loc("textures/entity/heli_rocket.png");
    }
}
