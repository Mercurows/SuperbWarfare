package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity.*;

public class AnnihilatorModel extends GeoModel<AnnihilatorEntity> {

    @Override
    public ResourceLocation getAnimationResource(AnnihilatorEntity entity) {
        return Mod.loc("animations/annihilator.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(AnnihilatorEntity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 64) {
            return Mod.loc("geo/annihilator.geo.json");
        } else {
            return Mod.loc("geo/annihilator.lod1.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(AnnihilatorEntity entity) {
        return Mod.loc("textures/entity/annihilator.png");
    }

    @Override
    public void setCustomAnimations(AnnihilatorEntity animatable, long instanceId, AnimationState<AnnihilatorEntity> animationState) {
        GeoBone laserLeft = getAnimationProcessor().getBone("laser1");
        GeoBone laserMiddle = getAnimationProcessor().getBone("laser2");
        GeoBone laserRight = getAnimationProcessor().getBone("laser3");

        laserLeft.setScaleZ(animatable.getEntityData().get(LASER_LEFT_LENGTH) + 0.5f);
        laserMiddle.setScaleZ(animatable.getEntityData().get(LASER_MIDDLE_LENGTH) + 0.5f);
        laserRight.setScaleZ(animatable.getEntityData().get(LASER_RIGHT_LENGTH) + 0.5f);

        GeoBone ledGreen = getAnimationProcessor().getBone("ledgreen");
        GeoBone ledGreen2 = getAnimationProcessor().getBone("ledgreen2");
        GeoBone ledGreen3 = getAnimationProcessor().getBone("ledgreen3");
        GeoBone ledGreen4 = getAnimationProcessor().getBone("ledgreen4");
        GeoBone ledGreen5 = getAnimationProcessor().getBone("ledgreen5");

        GeoBone ledRed = getAnimationProcessor().getBone("ledred");
        GeoBone ledRed2 = getAnimationProcessor().getBone("ledred2");
        GeoBone ledRed3 = getAnimationProcessor().getBone("ledred3");
        GeoBone ledRed4 = getAnimationProcessor().getBone("ledred4");
        GeoBone ledRed5 = getAnimationProcessor().getBone("ledred5");

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
