package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;

public class Ah6Model extends GeoModel<Ah6Entity> {

    @Override
    public ResourceLocation getAnimationResource(Ah6Entity entity) {
        return null;
//        return ModUtils.loc("animations/wheel_chair.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Ah6Entity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32) {
            return Mod.loc("geo/ah_6.geo.json");
        } else if (distance < 64) {
            return Mod.loc("geo/ah_6.lod1.geo.json");
        } else if (distance < 96) {
            return Mod.loc("geo/ah_6.lod2.geo.json");
        } else {
            return Mod.loc("geo/ah_6.lod3.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(Ah6Entity entity) {
        return Mod.loc("textures/entity/ah_6.png");
    }
}
