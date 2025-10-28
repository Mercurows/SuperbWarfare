package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public class Mle1934Model extends VehicleModel<Mle1934Entity> {

    @Override
    public void setCustomAnimations(Mle1934Entity animatable, long instanceId, AnimationState<Mle1934Entity> animationState) {
        var barrel = getAnimationProcessor().getBone("barrel");
        var entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        if (entityData != null) {
            barrel.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
        }
    }
}
