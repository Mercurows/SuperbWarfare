package com.atsuishio.superbwarfare.client.renderer.armor;

import com.atsuishio.superbwarfare.client.model.armor.UsHelmetPasgtModel;
import com.atsuishio.superbwarfare.item.armor.UsHelmetPasgt;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class UsHelmetPasgtArmorRenderer extends GeoArmorRenderer<UsHelmetPasgt> {
	public UsHelmetPasgtArmorRenderer() {
		super(new UsHelmetPasgtModel());
		this.head = new GeoBone(null, "", false, (double) 0, false, false);
	}

	@Override
	public RenderType getRenderType(UsHelmetPasgt animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
