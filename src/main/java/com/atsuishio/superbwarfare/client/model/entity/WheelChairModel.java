package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import software.bernie.geckolib.animation.AnimationState;

public class WheelChairModel extends VehicleModel<WheelChairEntity> {

    @Override
    public void setCustomAnimations(WheelChairEntity vehicle, long instanceId, AnimationState<WheelChairEntity> animationState) {
        super.setCustomAnimations(vehicle, instanceId, animationState);
        
        var wRB = getAnimationProcessor().getBone("w_rb");
        if (wRB != null) {
            wRB.setRotX(rightWheelRot);
        }

        var wLB = getAnimationProcessor().getBone("w_lb");
        if (wLB != null) {
            wLB.setRotX(leftWheelRot);
        }

        var wRR = getAnimationProcessor().getBone("w_rr");
        if (wRR != null) {
            wRR.setRotX(4 * rightWheelRot);
        }

        var wLR = getAnimationProcessor().getBone("w_lr");
        if (wLR != null) {
            wLR.setRotX(4 * leftWheelRot);
        }
    }
}
