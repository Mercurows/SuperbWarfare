package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.RgoGrenadeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RgoGrenadeEntityModel extends GeoModel<RgoGrenadeEntity> {

    @Override
    public ResourceLocation getAnimationResource(RgoGrenadeEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(RgoGrenadeEntity entity) {
        return Mod.loc("geo/rgo_grenade.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RgoGrenadeEntity entity) {
        return Mod.loc("textures/item/rgo_grenade.png");
    }
}
