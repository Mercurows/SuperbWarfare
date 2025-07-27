package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.MedicalKitEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MedicalKitModel extends GeoModel<MedicalKitEntity> {

    @Override
    public ResourceLocation getAnimationResource(MedicalKitEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(MedicalKitEntity entity) {
        return Mod.loc("geo/medical_kit.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MedicalKitEntity entity) {
        return Mod.loc("textures/entity/medical_kit.png");
    }
}
