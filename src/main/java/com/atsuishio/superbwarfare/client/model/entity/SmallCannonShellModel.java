package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class SmallCannonShellModel extends GeoModel<SmallCannonShellEntity> {

    @Override
    public ResourceLocation getAnimationResource(SmallCannonShellEntity entity) {
        return ModUtils.loc("animations/cannon_shell.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SmallCannonShellEntity entity) {
        return ModUtils.loc("geo/cannon_shell.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmallCannonShellEntity entity) {
        return ModUtils.loc("textures/entity/cannon_shell.png");
    }

    @Override
    public void setCustomAnimations(SmallCannonShellEntity animatable, long instanceId, AnimationState animationState) {
        GeoBone bone = getAnimationProcessor().getBone("bone");
        bone.setScaleX(0.17f);
        bone.setScaleY(0.17f);
        bone.setScaleZ(0.17f);
    }
}
