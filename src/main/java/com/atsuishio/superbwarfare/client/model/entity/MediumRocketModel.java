package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MediumRocketModel extends GeoModel<MediumRocketEntity> {

    @Override
    public ResourceLocation getAnimationResource(MediumRocketEntity entity) {
        return Mod.loc("animations/rpg_rocket.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MediumRocketEntity entity) {
        return Mod.loc("geo/medium_rocket.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MediumRocketEntity entity) {
        return Mod.loc("textures/entity/type_63.png");
    }
}
