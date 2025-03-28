package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.LungeMine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class LungeMineModel extends GeoModel<LungeMine> {

    @Override
    public ResourceLocation getAnimationResource(LungeMine animatable) {
        return ModUtils.loc("animations/lunge_mine.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(LungeMine animatable) {
        return ModUtils.loc("geo/lunge_mine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LungeMine animatable) {
        return ModUtils.loc("textures/item/lunge_mine.png");
    }

    @Override
    public void setCustomAnimations(LungeMine animatable, long instanceId, AnimationState animationState) {
        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
