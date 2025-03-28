package com.atsuishio.superbwarfare.client.model.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.armor.RuHelmet6b47;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RuHelmet6b47Model extends GeoModel<RuHelmet6b47> {

    @Override
    public ResourceLocation getAnimationResource(RuHelmet6b47 object) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(RuHelmet6b47 object) {
        return Mod.loc("geo/ru_helmet_6b47.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RuHelmet6b47 object) {
        return Mod.loc("textures/armor/ru_helmet_6b47.png");
    }
}
