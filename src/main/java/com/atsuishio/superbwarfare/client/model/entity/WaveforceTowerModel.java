package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.CHARGED_ENERGY;
import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.WAVEFORCE_LENGTH;

public class WaveforceTowerModel extends GeoModel<WaveforceTowerEntity> {

    @Override
    public ResourceLocation getAnimationResource(WaveforceTowerEntity entity) {
        return Mod.loc("animations/waveforce_tower.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(WaveforceTowerEntity entity) {
        return Mod.loc("geo/waveforce_tower.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WaveforceTowerEntity entity) {
        return Mod.loc("textures/entity/waveforce_tower.png");
    }

    @Override
    public void setCustomAnimations(WaveforceTowerEntity animatable, long instanceId, AnimationState<WaveforceTowerEntity> animationState) {
        GeoBone laser = getAnimationProcessor().getBone("laser");
        GeoBone glow = getAnimationProcessor().getBone("glow2");
        laser.setScaleZ(animatable.getEntityData().get(WAVEFORCE_LENGTH));
        glow.setPosZ(-16 * animatable.getEntityData().get(WAVEFORCE_LENGTH));


        GeoBone lightOn = getAnimationProcessor().getBone("light_on");
        GeoBone lightOn2 = getAnimationProcessor().getBone("light_on2");
        GeoBone lightOn3 = getAnimationProcessor().getBone("light_on3");
        GeoBone lightOn4 = getAnimationProcessor().getBone("light_on4");
        GeoBone lightOn5 = getAnimationProcessor().getBone("light_on5");
        GeoBone lightOn6 = getAnimationProcessor().getBone("light_on6");
        GeoBone lightOn7 = getAnimationProcessor().getBone("light_on7");

        GeoBone lightOff = getAnimationProcessor().getBone("light_off");
        GeoBone lightOff2 = getAnimationProcessor().getBone("light_off2");
        GeoBone lightOff3 = getAnimationProcessor().getBone("light_off3");
        GeoBone lightOff4 = getAnimationProcessor().getBone("light_off4");
        GeoBone lightOff5 = getAnimationProcessor().getBone("light_off5");
        GeoBone lightOff6 = getAnimationProcessor().getBone("light_off6");
        GeoBone lightOff7 = getAnimationProcessor().getBone("light_off7");

        float energy = animatable.getEntityData().get(CHARGED_ENERGY);
        float c0 = energy / animatable.maxChargeEnergy;

        lightOn.setHidden(c0 < 1 / 7f);
        lightOn2.setHidden(c0 < 2 / 7f);
        lightOn3.setHidden(c0 < 3 / 7f);
        lightOn4.setHidden(c0 < 4 / 7f);
        lightOn5.setHidden(c0 < 5 / 7f);
        lightOn6.setHidden(c0 < 6 / 7f);
        lightOn7.setHidden(c0 < 1f);

        lightOff.setHidden(!lightOn.isHidden());
        lightOff2.setHidden(!lightOn2.isHidden());
        lightOff3.setHidden(!lightOn3.isHidden());
        lightOff4.setHidden(!lightOn4.isHidden());
        lightOff5.setHidden(!lightOn5.isHidden());
        lightOff6.setHidden(!lightOn6.isHidden());
        lightOff7.setHidden(!lightOn7.isHidden());

        GeoBone charge = getAnimationProcessor().getBone("charge");
        charge.setScaleZ(c0);
    }
}
