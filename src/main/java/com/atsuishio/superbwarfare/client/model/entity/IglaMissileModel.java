package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IglaMissileModel extends GeoModel<IglaMissileEntity> {

    @Override
    public ResourceLocation getAnimationResource(IglaMissileEntity entity) {
        return Mod.loc("animations/javelin_missile.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(IglaMissileEntity entity) {
        return Mod.loc("geo/igla_9k38_missile.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(IglaMissileEntity entity) {
        return Mod.loc("textures/entity/igla_9k38.png");
    }
}
