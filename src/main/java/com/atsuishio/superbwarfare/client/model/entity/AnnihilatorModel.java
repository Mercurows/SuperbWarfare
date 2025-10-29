package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity.*;

public class AnnihilatorModel extends VehicleModel<AnnihilatorEntity> {

    @Override
    public void setCustomAnimations(AnnihilatorEntity animatable, long instanceId, AnimationState<AnnihilatorEntity> animationState) {
        CoreGeoBone laserLeft = getAnimationProcessor().getBone("laser1");
        CoreGeoBone laserMiddle = getAnimationProcessor().getBone("laser2");
        CoreGeoBone laserRight = getAnimationProcessor().getBone("laser3");

        laserLeft.setScaleZ(animatable.getEntityData().get(LASER_LEFT_LENGTH) + 0.5f);
        laserMiddle.setScaleZ(animatable.getEntityData().get(LASER_MIDDLE_LENGTH) + 0.5f);
        laserRight.setScaleZ(animatable.getEntityData().get(LASER_RIGHT_LENGTH) + 0.5f);

        CoreGeoBone ledGreen = getAnimationProcessor().getBone("ledgreen");
        CoreGeoBone ledGreen2 = getAnimationProcessor().getBone("ledgreen2");
        CoreGeoBone ledGreen3 = getAnimationProcessor().getBone("ledgreen3");
        CoreGeoBone ledGreen4 = getAnimationProcessor().getBone("ledgreen4");
        CoreGeoBone ledGreen5 = getAnimationProcessor().getBone("ledgreen5");

        CoreGeoBone ledRed = getAnimationProcessor().getBone("ledred");
        CoreGeoBone ledRed2 = getAnimationProcessor().getBone("ledred2");
        CoreGeoBone ledRed3 = getAnimationProcessor().getBone("ledred3");
        CoreGeoBone ledRed4 = getAnimationProcessor().getBone("ledred4");
        CoreGeoBone ledRed5 = getAnimationProcessor().getBone("ledred5");

        float coolDown = animatable.getEntityData().get(COOL_DOWN);
        boolean cantShoot = animatable.getEnergy() < VehicleConfig.ANNIHILATOR_SHOOT_COST.get();

        ledGreen.setHidden(coolDown > 80 || cantShoot);
        ledGreen2.setHidden(coolDown > 60 || cantShoot);
        ledGreen3.setHidden(coolDown > 40 || cantShoot);
        ledGreen4.setHidden(coolDown > 20 || cantShoot);
        ledGreen5.setHidden(coolDown > 0 || cantShoot);

        ledRed.setHidden(!ledGreen.isHidden());
        ledRed2.setHidden(!ledGreen2.isHidden());
        ledRed3.setHidden(!ledGreen3.isHidden());
        ledRed4.setHidden(!ledGreen4.isHidden());
        ledRed5.setHidden(!ledGreen5.isHidden());
    }
}
