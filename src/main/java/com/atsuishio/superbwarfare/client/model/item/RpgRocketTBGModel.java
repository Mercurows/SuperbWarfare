package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.common.ammo.RpgRocketTBG;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RpgRocketTBGModel extends GeoModel<RpgRocketTBG> {

    @Override
    public ResourceLocation getAnimationResource(RpgRocketTBG animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(RpgRocketTBG animatable) {
        return Mod.loc("geo/rpg_rocket_tbg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpgRocketTBG animatable) {
        return Mod.loc("textures/entity/rpg_rocket_tbg.png");
    }
}
