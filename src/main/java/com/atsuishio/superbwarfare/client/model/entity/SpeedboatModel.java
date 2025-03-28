package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;

public class SpeedboatModel extends GeoModel<SpeedboatEntity> {

    @Override
    public ResourceLocation getAnimationResource(SpeedboatEntity entity) {
        return Mod.loc("animations/speedboat.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SpeedboatEntity entity) {
        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32) {
            return Mod.loc("geo/speedboat.geo.json");
        } else {
            return Mod.loc("geo/speedboat.lod1.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(SpeedboatEntity entity) {
        return Mod.loc("textures/entity/speedboat.png");
    }
}
