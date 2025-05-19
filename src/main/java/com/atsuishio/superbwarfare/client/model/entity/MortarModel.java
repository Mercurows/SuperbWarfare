package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class MortarModel extends GeoModel<MortarEntity> {

    @Override
    public ResourceLocation getAnimationResource(MortarEntity entity) {
        return Mod.loc("animations/mortar.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MortarEntity entity) {
        return Mod.loc("geo/mortar.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MortarEntity entity) {
        return Mod.loc("textures/entity/mortar.png");
    }

    @Override
    public void setCustomAnimations(MortarEntity animatable, long instanceId, AnimationState<MortarEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("paoguan");
        GeoBone jiaojia = getAnimationProcessor().getBone("jiaojia");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
            jiaojia.setRotX(-2 * ((entityData.headPitch() - (10 - entityData.headPitch() * 0.1f)) * Mth.DEG_TO_RAD));
        }
    }
}
