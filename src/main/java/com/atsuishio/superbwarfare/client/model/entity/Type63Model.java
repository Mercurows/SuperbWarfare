package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.RenderHelper;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.model.GeoModel;

public class Type63Model extends GeoModel<Type63Entity> {

    @Override
    public ResourceLocation getAnimationResource(Type63Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Type63Entity entity) {
        if (RenderHelper.isInGui()) {
            return Mod.loc("geo/type_63.geo.json");
        }

        Player player = Minecraft.getInstance().player;

        int distance = 0;

        if (player != null) {
            distance = (int) player.position().distanceTo(entity.position());
        }

        if (distance < 32 || player.isScoping()) {
            return Mod.loc("geo/type_63.geo.json");
        } else if (distance < 96) {
            return Mod.loc("geo/vehicle_lod/type_63.lod1.geo.json");
        } else {
            return Mod.loc("geo/vehicle_lod/type_63.lod2.geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(Type63Entity entity) {
        return Mod.loc("textures/entity/type_63.png");
    }
}
