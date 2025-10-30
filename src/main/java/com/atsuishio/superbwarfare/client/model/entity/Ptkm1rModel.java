package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class Ptkm1rModel extends GeoModel<Ptkm1rEntity> {

    @Override
    public void setCustomAnimations(Ptkm1rEntity animatable, long instanceId, AnimationState<Ptkm1rEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        var body = getAnimationProcessor().getBone("body");
        if (body != null) {
            body.setRotY(-animatable.getYRot() * Mth.DEG_TO_RAD);
        }

        var zhu = getAnimationProcessor().getBone("zhu2");
        if (zhu != null) {
            zhu.setRotX(0.5f * Mth.lerp(animationState.getPartialTick(), animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD);
        }
    }

    @Override
    public ResourceLocation getAnimationResource(Ptkm1rEntity entity) {
        return Mod.loc("animations/ptkm_1r.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Ptkm1rEntity entity) {
        return Mod.loc("geo/ptkm_1r.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Ptkm1rEntity entity) {
        return Mod.loc("textures/entity/ptkm_1r.png");
    }
}
