package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.MilitaryShovel;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MilitaryShovelModel extends GeoModel<MilitaryShovel> {

    @Override
    public ResourceLocation getAnimationResource(MilitaryShovel animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(MilitaryShovel animatable) {
        return Mod.loc("geo/military_shovel.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MilitaryShovel animatable) {
        return Mod.loc("textures/item/military_shovel.png");
    }
}
