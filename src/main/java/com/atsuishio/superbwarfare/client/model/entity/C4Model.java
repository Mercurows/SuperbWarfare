package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.UUID;

public class C4Model extends GeoModel<C4Entity> {

    @Override
    public ResourceLocation getAnimationResource(C4Entity entity) {
        return ModUtils.loc("animations/c4.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(C4Entity entity) {
        return ModUtils.loc("geo/c4.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(C4Entity entity) {
        UUID uuid = entity.getUUID();
        if (uuid.getLeastSignificantBits() % 114 == 0) {
            return ModUtils.loc("textures/item/c4_alter.png");
        }
        return ModUtils.loc("textures/item/c4.png");
    }
}
