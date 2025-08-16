package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;

public class Bmp2Model extends GeoModel<Bmp2Entity> {

    @Override
    public ResourceLocation getAnimationResource(Bmp2Entity entity) {
        return Mod.loc("animations/lav.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Bmp2Entity entity) {
        if (RenderHelper.isInGui()) {
            return Mod.loc("geo/bmp2.geo.json");
        }

        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 48 || player.isScoping()) {
            return Mod.loc("geo/bmp2.geo.json");
        } else if (distance < 96) {
            return Mod.loc("geo/vehicle_lod/bmp2.lod1.geo.json");
        } else {
            return Mod.loc("geo/vehicle_lod/bmp2.lod2.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(Bmp2Entity entity) {
        return Mod.loc("textures/entity/bmp2.png");
    }
}
