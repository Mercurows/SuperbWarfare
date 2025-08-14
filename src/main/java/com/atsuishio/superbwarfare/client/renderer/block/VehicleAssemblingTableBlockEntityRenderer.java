package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock;
import com.atsuishio.superbwarfare.block.entity.VehicleAssemblingTableBlockEntity;
import com.atsuishio.superbwarfare.block.property.BlockPart;
import com.atsuishio.superbwarfare.client.model.block.VehicleAssemblingTableBlockModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class VehicleAssemblingTableBlockEntityRenderer extends GeoBlockRenderer<VehicleAssemblingTableBlockEntity> {

    public VehicleAssemblingTableBlockEntityRenderer() {
        super(new VehicleAssemblingTableBlockModel());
    }

    @Override
    public RenderType getRenderType(VehicleAssemblingTableBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public boolean shouldRender(VehicleAssemblingTableBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        return blockEntity.getBlockState().getValue(VehicleAssemblingTableBlock.BLOCK_PART) == BlockPart.FRB;
    }
}
