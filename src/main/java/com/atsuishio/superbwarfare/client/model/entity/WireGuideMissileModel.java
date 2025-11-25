package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WireGuideMissileModel extends GeoModel<WireGuideMissileEntity> {

    @Override
    public ResourceLocation getAnimationResource(WireGuideMissileEntity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(WireGuideMissileEntity entity) {
        return Mod.loc("geo/wire_guide_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WireGuideMissileEntity entity) {
        return Mod.loc("textures/entity/javelin_missile.png");
    }
}
