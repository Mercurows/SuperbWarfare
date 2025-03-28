package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Lav150Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Lav150Model extends GeoModel<Lav150Entity> {

    @Override
    public ResourceLocation getAnimationResource(Lav150Entity entity) {
        return Mod.loc("animations/lav.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Lav150Entity entity) {
        return Mod.loc("geo/lav150.geo.json");
//        Player player = Minecraft.getInstance().player;
//
//        int distance = 0;
//
//        if (player != null) {
//            distance = (int) player.position().distanceTo(entity.position());
//        }
//
//        if (distance < 32) {
//            return ModUtils.loc("geo/lav150.geo.json");
//        } else {
//            return ModUtils.loc("geo/speedboat.lod1.geo.json");
//        }
    }

    @Override
    public ResourceLocation getTextureResource(Lav150Entity entity) {
        return Mod.loc("textures/entity/lav150.png");
    }
}
