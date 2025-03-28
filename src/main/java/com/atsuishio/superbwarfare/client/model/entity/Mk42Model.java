package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Mk42Model extends GeoModel<Mk42Entity> {

    @Override
    public ResourceLocation getAnimationResource(Mk42Entity entity) {
        return Mod.loc("animations/mk_42.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Mk42Entity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32) {
            return Mod.loc("geo/sherman.geo.json");
        } else if (distance < 64) {
            return Mod.loc("geo/sherman_lod1.geo.json");
        } else {
            return Mod.loc("geo/sherman_lod2.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(Mk42Entity entity) {
        return Mod.loc("textures/entity/mk42.png");
    }

    @Override
    public void setCustomAnimations(Mk42Entity animatable, long instanceId, AnimationState<Mk42Entity> animationState) {
        GeoBone bone = getAnimationProcessor().getBone("maingun");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        bone.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
    }
}
