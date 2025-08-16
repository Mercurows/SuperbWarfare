package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Lav150Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;

public class Lav150Model extends GeoModel<Lav150Entity> {

    @Override
    public ResourceLocation getAnimationResource(Lav150Entity entity) {
        return Mod.loc("animations/lav.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Lav150Entity entity) {
        if (RenderHelper.isInGui()) {
            return Mod.loc("geo/lav150.geo.json");
        }

        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32 || player.isScoping()) {
            return Mod.loc("geo/lav150.geo.json");
        } else if (distance < 96) {
            return Mod.loc("geo/vehicle_lod/lav150.lod1.geo.json");
        } else {
            return Mod.loc("geo/vehicle_lod/lav150.lod2.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(Lav150Entity entity) {
        if (RenderHelper.isInGui()) {
            return Mod.loc("textures/entity/lav150.png");
        }

        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32 || player.isScoping()) {
            return Mod.loc("textures/entity/lav150.png");
        } else if (distance < 96) {
            return Mod.loc("textures/entity/lav150_lod1.png");
        } else {
            return Mod.loc("textures/entity/lav150_lod2.png");
        }
    }
}
