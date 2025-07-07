package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.M18SmokeGrenadeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class M18SmokeGrenadeEntityModel extends GeoModel<M18SmokeGrenadeEntity> {

    @Override
    public ResourceLocation getAnimationResource(M18SmokeGrenadeEntity entity) {
        return null;
    }

    // TODO 添加烟雾手雷资源文件

    @Override
    public ResourceLocation getModelResource(M18SmokeGrenadeEntity entity) {
        return Mod.loc("geo/m18_smoke_grenade.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M18SmokeGrenadeEntity entity) {
        return Mod.loc("textures/item/m_18.png");
    }
}
