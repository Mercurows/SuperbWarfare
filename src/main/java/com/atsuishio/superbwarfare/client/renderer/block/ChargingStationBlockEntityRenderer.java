package com.atsuishio.superbwarfare.client.renderer.block;

import com.atsuishio.superbwarfare.block.ChargingStationBlock;
import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity;
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
public class ChargingStationBlockEntityRenderer implements BlockEntityRenderer<ChargingStationBlockEntity> {

    @Override
    public void render(ChargingStationBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.getBlockState().getValue(ChargingStationBlock.SHOW_RANGE)) return;

        poseStack.pushPose();
        var pos = blockEntity.getBlockPos();
        poseStack.translate(-pos.getX(), -pos.getY(), -pos.getZ());

        var aabb = new AABB(pos).inflate(ChargingStationBlockEntity.CHARGE_RADIUS);

        float startX = (float) aabb.minX - 0.001f;
        float startY = (float) aabb.minY - 0.001f;
        float startZ = (float) aabb.minZ - 0.001f;
        float endX = (float) aabb.maxX + 0.001f;
        float endY = (float) aabb.maxY + 0.001f;
        float endZ = (float) aabb.maxZ + 0.001f;

        var red = 0.0f;
        var green = 1.0f;
        var blue = 0.0f;
        var alpha = 0.2f;


        var builder = bufferSource.getBuffer(ModRenderTypes.BLOCK_OVERLAY);
        var m4f = poseStack.last().pose();

        // east
        builder.addVertex(m4f, startX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, startY, startZ).setColor(red, green, blue, alpha);

        // west
        builder.addVertex(m4f, startX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, endY, endZ).setColor(red, green, blue, alpha);

        // south
        builder.addVertex(m4f, endX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, startY, endZ).setColor(red, green, blue, alpha);

        // north
        builder.addVertex(m4f, startX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, endY, startZ).setColor(red, green, blue, alpha);

        // top
        builder.addVertex(m4f, startX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, endY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, endY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, endY, endZ).setColor(red, green, blue, alpha);

        // bottom
        builder.addVertex(m4f, startX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, startY, startZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, endX, startY, endZ).setColor(red, green, blue, alpha);
        builder.addVertex(m4f, startX, startY, endZ).setColor(red, green, blue, alpha);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull ChargingStationBlockEntity blockEntity) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean shouldRender(ChargingStationBlockEntity blockEntity, Vec3 pCameraPos) {
        return true;
    }

}
