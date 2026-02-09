package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TurretWreckModel extends GeoModel<TurretWreckEntity> {

    @Override
    public ResourceLocation getAnimationResource(TurretWreckEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(TurretWreckEntity entity) {
        return Mod.loc("geo/gun_mu.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TurretWreckEntity entity) {
        return Mod.loc("textures/entity/empty.png");
    }
}
