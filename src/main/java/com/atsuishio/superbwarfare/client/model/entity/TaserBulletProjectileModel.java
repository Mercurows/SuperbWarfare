package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TaserBulletProjectileModel extends GeoModel<TaserBulletEntity> {

    @Override
    public ResourceLocation getAnimationResource(TaserBulletEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(TaserBulletEntity entity) {
        return Mod.loc("geo/taser_rod.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TaserBulletEntity entity) {
        return Mod.loc("textures/entity/taser_rod.png");
    }
}
