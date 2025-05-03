package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class SmallCannonShellModel extends GeoModel<SmallCannonShellEntity> {

    @Override
    public ResourceLocation getAnimationResource(SmallCannonShellEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(SmallCannonShellEntity entity) {
        return Mod.loc("geo/small_cannon_shell.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SmallCannonShellEntity entity) {
        return Mod.loc("textures/entity/small_cannon_shell.png");
    }

    @Override
    public void setCustomAnimations(SmallCannonShellEntity animatable, long instanceId, AnimationState animationState) {
        GeoBone bone = getAnimationProcessor().getBone("bone");
        bone.setScaleY((float) (1 + 2 * animatable.getDeltaMovement().length()));
    }
}
