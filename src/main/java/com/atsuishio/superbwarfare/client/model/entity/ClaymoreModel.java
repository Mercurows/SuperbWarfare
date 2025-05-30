package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.ClaymoreEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import java.util.UUID;

public class ClaymoreModel extends GeoModel<ClaymoreEntity> {

    @Override
    public ResourceLocation getAnimationResource(ClaymoreEntity entity) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(ClaymoreEntity entity) {
        return Mod.loc("geo/claymore.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ClaymoreEntity entity) {
        UUID uuid = entity.getUUID();
        if (uuid.getLeastSignificantBits() % 514 == 0) {
            return Mod.loc("textures/entity/claymore_alter.png");
        }
        return Mod.loc("textures/entity/claymore.png");
    }
}
