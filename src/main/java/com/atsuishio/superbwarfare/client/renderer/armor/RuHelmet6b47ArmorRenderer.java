package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.RuHelmet6b47Model;
import com.atsuishio.superbwarfare.item.armor.RuHelmet6b47;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class RuHelmet6b47ArmorRenderer extends GeoArmorRenderer<RuHelmet6b47> {
    public RuHelmet6b47ArmorRenderer() {
        super(new RuHelmet6b47Model());
        this.head = new GeoBone(null, "", false, (double) 0, false, false);
    }

    @Override
    public RenderType getRenderType(RuHelmet6b47 animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
