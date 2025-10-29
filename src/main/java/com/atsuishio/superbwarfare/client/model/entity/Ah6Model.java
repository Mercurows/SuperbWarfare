package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class Ah6Model extends VehicleModel<Ah6Entity> {

    @Override
    public void setCustomAnimations(Ah6Entity vehicle, long instanceId, AnimationState<Ah6Entity> animationState) {
        super.setCustomAnimations(vehicle, instanceId, animationState);
        float partialTick = Minecraft.getInstance().getPartialTick();

        CoreGeoBone propeller = getAnimationProcessor().getBone("propeller");

        if (propeller != null) {
            propeller.setRotY(Mth.lerp(partialTick, vehicle.propellerRotO, vehicle.getPropellerRot()));
        }

        CoreGeoBone tailPropeller = getAnimationProcessor().getBone("tailPropeller");

        if (tailPropeller != null) {
            tailPropeller.setRotX(-6 * Mth.lerp(partialTick, vehicle.propellerRotO, vehicle.getPropellerRot()));
        }
    }
}
