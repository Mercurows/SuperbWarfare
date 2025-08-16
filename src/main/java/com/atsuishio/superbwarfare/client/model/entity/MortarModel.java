package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class MortarModel extends GeoModel<MortarEntity> {

    @Override
    public ResourceLocation getAnimationResource(MortarEntity entity) {
        return Mod.loc("animations/mortar.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MortarEntity entity) {
        if (RenderHelper.isInGui()) {
            return Mod.loc("geo/mortar.geo.json");
        }

        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 48 || player.isScoping()) {
            return Mod.loc("geo/mortar.geo.json");
        } else  {
            return Mod.loc("geo/vehicle_lod/mortar.lod1.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(MortarEntity entity) {
        return Mod.loc("textures/entity/mortar.png");
    }

    @Override
    public void setCustomAnimations(MortarEntity animatable, long instanceId, AnimationState<MortarEntity> animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("paoguan");
        CoreGeoBone jiaojia = getAnimationProcessor().getBone("jiaojia");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
            jiaojia.setRotX(-2 * ((entityData.headPitch() - (10 - entityData.headPitch() * 0.1f)) * Mth.DEG_TO_RAD));
        }
    }
}
