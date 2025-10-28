package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;

public class Mk42Model extends VehicleModel<Mk42Entity> {

    @Override
    public void setCustomAnimations(Mk42Entity animatable, long instanceId, AnimationState<Mk42Entity> animationState) {
        var bone = getAnimationProcessor().getBone("maingun");
        var entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        if (entityData != null) {
            bone.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
        }
    }
}
