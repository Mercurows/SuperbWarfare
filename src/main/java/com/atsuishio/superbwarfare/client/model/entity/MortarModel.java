package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public class MortarModel extends VehicleModel<MortarEntity> {

    @Override
    public void setCustomAnimations(MortarEntity animatable, long instanceId, AnimationState<MortarEntity> animationState) {
        var head = getAnimationProcessor().getBone("paoguan");
        var jiaojia = getAnimationProcessor().getBone("jiaojia");
        if (head != null) {
            var entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            if (entityData != null) {
                head.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
                jiaojia.setRotX(-2 * ((entityData.headPitch() - (10 - entityData.headPitch() * 0.1f)) * Mth.DEG_TO_RAD));
            }
        }
    }
}
