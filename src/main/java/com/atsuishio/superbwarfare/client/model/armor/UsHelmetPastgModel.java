package com.atsuishio.superbwarfare.client.model.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.armor.UsHelmetPastg;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UsHelmetPastgModel extends GeoModel<UsHelmetPastg> {

    @Override
    public ResourceLocation getAnimationResource(UsHelmetPastg object) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(UsHelmetPastg object) {
        return Mod.loc("geo/us_helmet_pastg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(UsHelmetPastg object) {
        return Mod.loc("textures/armor/us_helmet_pastg.png");
    }
}
