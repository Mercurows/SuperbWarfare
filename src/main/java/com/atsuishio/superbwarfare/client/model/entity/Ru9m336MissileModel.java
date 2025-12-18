package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.Ru9m336MissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Ru9m336MissileModel extends GeoModel<Ru9m336MissileEntity> {

    @Override
    public ResourceLocation getAnimationResource(Ru9m336MissileEntity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Ru9m336MissileEntity entity) {
        return Mod.loc("geo/igla_9k38_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Ru9m336MissileEntity entity) {
        return Mod.loc("textures/entity/igla_9k38.png");
    }
}
