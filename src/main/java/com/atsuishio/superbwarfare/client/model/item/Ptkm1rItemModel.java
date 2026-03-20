package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.projectile.Ptkm1rItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Ptkm1rItemModel extends GeoModel<Ptkm1rItem> {

    @Override
    public ResourceLocation getAnimationResource(Ptkm1rItem animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(Ptkm1rItem animatable) {
        return Mod.loc("geo/ptkm_1r_item.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Ptkm1rItem animatable) {
        return Mod.loc("textures/item/ptkm_1r.png");
    }
}
