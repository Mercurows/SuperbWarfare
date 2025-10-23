package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SteelCoilEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SteelCoilModel extends GeoModel<SteelCoilEntity> {

    @Override
    public ResourceLocation getAnimationResource(SteelCoilEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(SteelCoilEntity entity) {
        return Mod.loc("geo/steel_coil.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SteelCoilEntity entity) {
        return Mod.loc("textures/entity/steel_coil.png");
    }
}
