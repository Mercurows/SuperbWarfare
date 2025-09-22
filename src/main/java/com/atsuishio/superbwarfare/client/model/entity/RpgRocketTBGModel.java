package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RpgRocketTBGModel extends GeoModel<RpgRocketTBGEntity> {

    @Override
    public ResourceLocation getAnimationResource(RpgRocketTBGEntity entity) {
        return Mod.loc("animations/rpg_rocket.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RpgRocketTBGEntity entity) {
        return Mod.loc("geo/rpg_rocket_head_tbg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpgRocketTBGEntity entity) {
        return Mod.loc("textures/entity/rpg_rocket_tbg.png");
    }
}
