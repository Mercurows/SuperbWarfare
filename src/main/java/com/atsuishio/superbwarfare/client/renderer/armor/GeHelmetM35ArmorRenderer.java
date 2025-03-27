package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.GeHelmetM35Model;
import com.atsuishio.superbwarfare.item.armor.GeHelmetM35;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class GeHelmetM35ArmorRenderer extends GeoArmorRenderer<GeHelmetM35> {
    public GeHelmetM35ArmorRenderer() {
        super(new GeHelmetM35Model());
        this.head = new GeoBone(null, "", false, (double) 0, false, false);
    }

    @Override
    public RenderType getRenderType(GeHelmetM35 animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
