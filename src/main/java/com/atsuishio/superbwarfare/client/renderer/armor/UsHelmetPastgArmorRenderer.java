package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.UsHelmetPastgModel;
import com.atsuishio.superbwarfare.item.armor.UsHelmetPastg;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class UsHelmetPastgArmorRenderer extends GeoArmorRenderer<UsHelmetPastg> {
    public UsHelmetPastgArmorRenderer() {
        super(new UsHelmetPastgModel());
        this.head = new GeoBone(null, "", false, (double) 0, false, false);
    }

    @Override
    public RenderType getRenderType(UsHelmetPastg animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
