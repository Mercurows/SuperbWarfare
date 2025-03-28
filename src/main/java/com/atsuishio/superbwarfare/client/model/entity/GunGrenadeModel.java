package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class GunGrenadeModel extends GeoModel<GunGrenadeEntity> {

    @Override
    public ResourceLocation getAnimationResource(GunGrenadeEntity entity) {
        return Mod.loc("animations/cannon_shell.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(GunGrenadeEntity entity) {
        return Mod.loc("geo/cannon_shell.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GunGrenadeEntity entity) {
        return Mod.loc("textures/entity/cannon_shell.png");
    }

    @Override
    public void setCustomAnimations(GunGrenadeEntity animatable, long instanceId, AnimationState animationState) {
        GeoBone bone = getAnimationProcessor().getBone("bone");
        bone.setScaleX(0.2f);
        bone.setScaleY(0.2f);
        bone.setScaleZ(0.2f);
    }
}
