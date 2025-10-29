package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;

public class SpeedboatModel extends VehicleModel<SpeedboatEntity> {

    @Override
    public void setCustomAnimations(SpeedboatEntity vehicle, long instanceId, AnimationState<SpeedboatEntity> animationState) {
        super.setCustomAnimations(vehicle, instanceId, animationState);
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        var propeller = getAnimationProcessor().getBone("propeller");

        if (propeller != null) {
            propeller.setRotZ(Mth.lerp(partialTick, vehicle.propellerRotO, vehicle.getPropellerRot()));
        }

        var rudder = getAnimationProcessor().getBone("rudder");

        if (rudder != null) {
            rudder.setRotY(Mth.lerp(partialTick, vehicle.rudderRotO, vehicle.getRudderRot()));
        }
    }
}
