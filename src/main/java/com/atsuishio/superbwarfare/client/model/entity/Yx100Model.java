package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Yx100Model extends GeoModel<Yx100Entity> {

    @Override
    public ResourceLocation getAnimationResource(Yx100Entity entity) {
        return Mod.loc("animations/yx_100.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Yx100Entity entity) {
        return Mod.loc("geo/yx_100.geo.json");
//        Player player = Minecraft.getInstance().player;
//
//        int distance = 0;
//
//        if (player != null) {
//            distance = (int) player.position().distanceTo(entity.position());
//        }
//
//        if (distance < 32) {
//            return ModUtils.loc("geo/Yx100.geo.json");
//        } else {
//            return ModUtils.loc("geo/speedboat.lod1.geo.json");
//        }
    }

    @Override
    public ResourceLocation getTextureResource(Yx100Entity entity) {
        return Mod.loc("textures/entity/yx_100.png");
    }
}
