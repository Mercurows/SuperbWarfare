package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.model.item.JavelinItemModel;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class JavelinItemRenderer extends GeoItemRenderer<JavelinItem> {

    public JavelinItemRenderer() {
        super(new JavelinItemModel());
    }

    @Override
    public RenderType getRenderType(JavelinItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public ResourceLocation getTextureLocation(JavelinItem instance) {
        return super.getTextureLocation(instance);
    }
}
