package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.common.ammo.RpgRocketStandard;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RpgRocketStandardModel extends GeoModel<RpgRocketStandard> {

    @Override
    public ResourceLocation getAnimationResource(RpgRocketStandard animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(RpgRocketStandard animatable) {
        return Mod.loc("geo/rpg_rocket_standard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpgRocketStandard animatable) {
        return Mod.loc("textures/item/rpg.png");
    }
}
