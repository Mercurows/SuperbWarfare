package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.PtkmProjectileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Ptkm1rProjectileModel extends GeoModel<PtkmProjectileEntity> {

    @Override
    public ResourceLocation getAnimationResource(PtkmProjectileEntity entity) {
        return Mod.loc("animations/ptkm_1r.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(PtkmProjectileEntity entity) {
        return Mod.loc("geo/ptkm_1r.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PtkmProjectileEntity entity) {
        return Mod.loc("textures/entity/ptkm_1r.png");
    }
}
