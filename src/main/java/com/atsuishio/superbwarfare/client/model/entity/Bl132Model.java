package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Bl132Model extends GeoModel<Bl132Entity> {

    @Override
    public ResourceLocation getAnimationResource(Bl132Entity entity) {
        return Mod.loc("animations/bl_132.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Bl132Entity entity) {
        return Mod.loc("geo/bl_132.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Bl132Entity entity) {
        return Mod.loc("textures/entity/bl_132.png");
    }

    @Override
    public void setCustomAnimations(Bl132Entity animatable, long instanceId, AnimationState<Bl132Entity> animationState) {
        CoreGeoBone bone = getAnimationProcessor().getBone("gun");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        bone.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
    }
}
