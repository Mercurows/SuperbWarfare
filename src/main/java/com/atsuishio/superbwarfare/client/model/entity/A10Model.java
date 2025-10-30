package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class A10Model extends VehicleModel<A10Entity> {

    @Override
    public void setCustomAnimations(A10Entity vehicle, long instanceId, AnimationState<A10Entity> animationState) {
        CoreGeoBone root = getAnimationProcessor().getBone("root");

        if (root != null && hideFor1stPassengerWhileZooming()) {
            root.setHidden(hideFor1stPassengerWhileZooming && vehicle.getWeaponIndex(0) == 2);
        }
    }
}
