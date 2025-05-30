package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.UUID;

public class C4Model extends GeoModel<C4Entity> {

    @Override
    public ResourceLocation getAnimationResource(C4Entity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(C4Entity entity) {
        return Mod.loc("geo/c4.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(C4Entity entity) {
        UUID uuid = entity.getUUID();
        if (uuid.getLeastSignificantBits() % 114 == 0) {
            return Mod.loc("textures/item/c4_alter.png");
        }
        return Mod.loc("textures/item/c4.png");
    }
}
