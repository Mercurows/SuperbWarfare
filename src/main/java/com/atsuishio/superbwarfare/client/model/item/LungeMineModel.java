package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.LungeMine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class LungeMineModel extends GeoModel<LungeMine> {

    @Override
    public ResourceLocation getAnimationResource(LungeMine animatable) {
        return Mod.loc("animations/lunge_mine.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(LungeMine animatable) {
        return Mod.loc("geo/lunge_mine.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LungeMine animatable) {
        return Mod.loc("textures/item/lunge_mine.png");
    }

    @Override
    public void setCustomAnimations(LungeMine animatable, long instanceId, AnimationState animationState) {
        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
