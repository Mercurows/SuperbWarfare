package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RpgRocketStandardModel extends GeoModel<RpgRocketStandardEntity> {

    @Override
    public ResourceLocation getAnimationResource(RpgRocketStandardEntity entity) {
        return Mod.loc("animations/rpg_rocket.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RpgRocketStandardEntity entity) {
        return Mod.loc("geo/rpg_rocket_head_standard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpgRocketStandardEntity entity) {
        return Mod.loc("textures/entity/rpg_rocket_standard.png");
    }
}
