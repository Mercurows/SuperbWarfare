package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RpgRocketModel extends GeoModel<RpgRocketEntity> {

    @Override
    public ResourceLocation getAnimationResource(RpgRocketEntity entity) {
        return Mod.loc("animations/rpg_rocket.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RpgRocketEntity entity) {
        return Mod.loc("geo/rpg_rocket.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpgRocketEntity entity) {
        return Mod.loc("textures/entity/rpg_rocket.png");
    }
}
