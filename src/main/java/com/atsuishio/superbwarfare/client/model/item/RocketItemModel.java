package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.common.ammo.Rocket;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RocketItemModel extends GeoModel<Rocket> {

    @Override
    public ResourceLocation getAnimationResource(Rocket animatable) {
        return Mod.loc("animations/rpg.head.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Rocket animatable) {
        return Mod.loc("geo/rpg.head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Rocket animatable) {
        return Mod.loc("textures/entity/rpg_rocket.png");
    }
}
