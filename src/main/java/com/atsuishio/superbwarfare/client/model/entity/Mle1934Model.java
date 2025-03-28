package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Mle1934Model extends GeoModel<Mle1934Entity> {

    @Override
    public ResourceLocation getAnimationResource(Mle1934Entity entity) {
        return Mod.loc("animations/mle1934.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Mle1934Entity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;
        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32) {
            return Mod.loc("geo/mle1934.geo.json");
        } else {
            return Mod.loc("geo/mle1934_lod1.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(Mle1934Entity entity) {
        return Mod.loc("textures/entity/mle1934.png");
    }

    @Override
    public void setCustomAnimations(Mle1934Entity animatable, long instanceId, AnimationState<Mle1934Entity> animationState) {
        GeoBone barrel = getAnimationProcessor().getBone("barrel");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        barrel.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
    }
}
