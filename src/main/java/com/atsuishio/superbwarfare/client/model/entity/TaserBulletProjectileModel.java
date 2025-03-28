package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.ModUtils;
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
        return ModUtils.loc("geo/taser_rod.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TaserBulletEntity entity) {
        return ModUtils.loc("textures/entity/taser_rod.png");
    }
}
