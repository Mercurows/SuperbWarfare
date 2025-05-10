package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;

public class DPSGeneratorModel extends GeoModel<DPSGeneratorEntity> {

    @Override
    public ResourceLocation getAnimationResource(DPSGeneratorEntity entity) {
        return Mod.loc("animations/dps_generator.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(DPSGeneratorEntity entity) {
        return Mod.loc("geo/dps_generator.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DPSGeneratorEntity entity) {
        return Mod.loc("textures/entity/dps_generator_tier_" + Mth.clamp(entity.getGeneratorLevel(), 0, 7) + ".png");
    }
}
