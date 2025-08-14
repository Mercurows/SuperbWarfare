package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity;
import com.atsuishio.superbwarfare.client.model.block.VehicleAssemblingTableBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class VehicleAssemblingTableBlockEntityRenderer extends GeoBlockRenderer<VehicleAssemblingTableBlockEntity> {

	public VehicleAssemblingTableBlockEntityRenderer() {
		super(new VehicleAssemblingTableBlockModel());
	}

	@Override
	public RenderType getRenderType(VehicleAssemblingTableBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
