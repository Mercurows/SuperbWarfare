package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import static com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity.*;

public class AnnihilatorModel extends GeoModel<AnnihilatorEntity> {

    @Override
    public ResourceLocation getAnimationResource(AnnihilatorEntity entity) {
        return ModUtils.loc("animations/annihilator.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(AnnihilatorEntity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 64) {
            return ModUtils.loc("geo/annihilator.geo.json");
        } else {
            return ModUtils.loc("geo/annihilator.lod1.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(AnnihilatorEntity entity) {
        return ModUtils.loc("textures/entity/annihilator.png");
    }

    @Override
    public void setCustomAnimations(AnnihilatorEntity animatable, long instanceId, AnimationState<AnnihilatorEntity> animationState) {
        CoreGeoBone bone = getAnimationProcessor().getBone("PaoGuan");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        bone.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);

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
        boolean cantShoot = animatable.getEnergy() < 20;

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
