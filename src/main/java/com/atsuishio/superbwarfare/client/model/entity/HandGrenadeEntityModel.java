package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.projectile.HandGrenadeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HandGrenadeEntityModel extends GeoModel<HandGrenadeEntity> {

    @Override
    public ResourceLocation getAnimationResource(HandGrenadeEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(HandGrenadeEntity entity) {
        return ModUtils.loc("geo/hand_grenade.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HandGrenadeEntity entity) {
        return ModUtils.loc("textures/entity/hand_grenade.png");
    }
}
