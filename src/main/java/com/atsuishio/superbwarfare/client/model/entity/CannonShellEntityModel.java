package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class CannonShellEntityModel extends GeoModel<CannonShellEntity> {

    @Override
    public ResourceLocation getAnimationResource(CannonShellEntity entity) {
        return Mod.loc("animations/cannon_shell.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(CannonShellEntity entity) {
        return Mod.loc("geo/cannon_shell.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CannonShellEntity entity) {
        return Mod.loc("textures/entity/cannon_shell.png");
    }

    @Override
    public void setCustomAnimations(CannonShellEntity animatable, long instanceId, AnimationState animationState) {
        GeoBone bone = getAnimationProcessor().getBone("bone");
        bone.setHidden(animatable.tickCount <= 1);
    }
}
