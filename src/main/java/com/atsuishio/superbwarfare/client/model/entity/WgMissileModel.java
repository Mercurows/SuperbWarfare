package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.WgMissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WgMissileModel extends GeoModel<WgMissileEntity> {

    @Override
    public ResourceLocation getAnimationResource(WgMissileEntity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(WgMissileEntity entity) {
        return Mod.loc("geo/wg_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WgMissileEntity entity) {
        return Mod.loc("textures/entity/javelin_missile.png");
    }
}
