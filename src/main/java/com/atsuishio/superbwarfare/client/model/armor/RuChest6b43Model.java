package com.atsuishio.superbwarfare.client.model.armor;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.armor.RuChest6b43;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RuChest6b43Model extends GeoModel<RuChest6b43> {

    @Override
    public ResourceLocation getAnimationResource(RuChest6b43 object) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(RuChest6b43 object) {
        return Mod.loc("geo/ru_chest_6b43.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RuChest6b43 object) {
        return Mod.loc("textures/armor/ru_chest_6b43.png");
    }
}
