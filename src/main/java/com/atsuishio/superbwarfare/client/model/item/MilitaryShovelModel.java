package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.weapon.MilitaryShovelItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MilitaryShovelModel extends GeoModel<MilitaryShovelItem> {

    @Override
    public ResourceLocation getAnimationResource(MilitaryShovelItem animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(MilitaryShovelItem animatable) {
        return Mod.loc("geo/military_shovel.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MilitaryShovelItem animatable) {
        return Mod.loc("textures/item/military_shovel.png");
    }
}
