package com.atsuishio.superbwarfare.client.model.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.armor.UsChestIotv;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UsChestIotvModel extends GeoModel<UsChestIotv> {

    @Override
    public ResourceLocation getAnimationResource(UsChestIotv object) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(UsChestIotv object) {
        return Mod.loc("geo/us_chest_iotv.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(UsChestIotv object) {
        return Mod.loc("textures/armor/us_chest_iotv.png");
    }
}
