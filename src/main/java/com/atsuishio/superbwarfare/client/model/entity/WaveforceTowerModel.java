package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.CHARGED_ENERGY;
import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.WAVEFORCE_LENGTH;

public class WaveforceTowerModel extends VehicleModel<WaveforceTowerEntity> {

    int energy0 = 0;

    // TODO 修复渲染问题
    @Override
    public void setCustomAnimations(WaveforceTowerEntity animatable, long instanceId, AnimationState<WaveforceTowerEntity> animationState) {
        var processor = getAnimationProcessor();
        var entityData = animatable.getEntityData();

        processor.getBone("laser").setScaleZ(entityData.get(WAVEFORCE_LENGTH));
        processor.getBone("glow2").setPosZ(-16 * entityData.get(WAVEFORCE_LENGTH));

        int energy = entityData.get(CHARGED_ENERGY);
        float energyRate = (float) energy / animatable.maxChargeEnergy;
        float energyRate0 = (float) energy0 / animatable.maxChargeEnergy;

        for (int i = 1; i <= 7; i++) {
            var lightOn = processor.getBone("light_on" + i);
            var lightOff = processor.getBone("light_off" + i);

            var shouldTurnOn = energyRate >= i / 7f;
            lightOff.setHidden(shouldTurnOn);
            lightOn.setHidden(!shouldTurnOn);
        }

        processor.getBone("charge").setScaleZ(Mth.lerp(Minecraft.getInstance().getDeltaFrameTime(), energyRate0, energyRate));

        energy0 = energy;
    }
}
