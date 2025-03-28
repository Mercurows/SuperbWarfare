package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Bmp2Model extends GeoModel<Bmp2Entity> {

    @Override
    public ResourceLocation getAnimationResource(Bmp2Entity entity) {
        return Mod.loc("animations/lav.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Bmp2Entity entity) {
        return Mod.loc("geo/bmp2.geo.json");
//        Player player = Minecraft.getInstance().player;
//
//        int distance = 0;
//
//        if (player != null) {
//            distance = (int) player.position().distanceTo(entity.position());
//        }
//
//        if (distance < 32) {
//            return ModUtils.loc("geo/Bmp2.geo.json");
//        } else {
//            return ModUtils.loc("geo/speedboat.lod1.geo.json");
//        }
    }

    @Override
    public ResourceLocation getTextureResource(Bmp2Entity entity) {
        return Mod.loc("textures/entity/bmp2.png");
    }
}
