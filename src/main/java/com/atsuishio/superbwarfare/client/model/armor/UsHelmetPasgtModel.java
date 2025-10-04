package com.atsuishio.superbwarfare.client.model.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.armor.UsHelmetPasgt;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UsHelmetPasgtModel extends GeoModel<UsHelmetPasgt> {

    @Override
    public ResourceLocation getAnimationResource(UsHelmetPasgt object) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(UsHelmetPasgt object) {
        return Mod.loc("geo/us_helmet_pasgt.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(UsHelmetPasgt object) {
        return Mod.loc("textures/armor/us_helmet_pasgt.png");
    }
}
