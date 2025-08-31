package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity.CHARGING_TIME;

public class WaveforceTowerModel extends GeoModel<WaveforceTowerEntity> {

    @Override
    public ResourceLocation getAnimationResource(WaveforceTowerEntity entity) {
        return null;
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
//        GeoBone waveForce = getAnimationProcessor().getBone("waveForce");
//        waveForce.setScaleZ(10 * animatable.getEntityData().get(WAVEFORCE_LENGTH));
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

        float coolDown = animatable.getEntityData().get(CHARGING_TIME);

        lightOn.setHidden(coolDown < 12);
        lightOn2.setHidden(coolDown < 20);
        lightOn3.setHidden(coolDown < 28);
        lightOn4.setHidden(coolDown < 36);
        lightOn5.setHidden(coolDown < 44);
        lightOn6.setHidden(coolDown < 52);
        lightOn7.setHidden(coolDown < 60);

        lightOff.setHidden(!lightOn.isHidden());
        lightOff2.setHidden(!lightOn2.isHidden());
        lightOff3.setHidden(!lightOn3.isHidden());
        lightOff4.setHidden(!lightOn4.isHidden());
        lightOff5.setHidden(!lightOn5.isHidden());
        lightOff6.setHidden(!lightOn6.isHidden());
        lightOff7.setHidden(!lightOn7.isHidden());
    }
}
