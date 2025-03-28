package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class MelonBombEntityRenderer extends EntityRenderer<MelonBombEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public MelonBombEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2f;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(@NotNull MelonBombEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        matrixStack.pushPose();
        matrixStack.translate(0.0, 0.5, 0.0);
        matrixStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
        matrixStack.translate(-0.5, -0.5, 0.5);
        matrixStack.mulPose(Axis.YP.rotationDegrees(90.0f));
        TntMinecartRenderer.renderWhiteSolidBlock(this.blockRenderer, Blocks.MELON.defaultBlockState(), matrixStack, buffer, packedLight, false);
        matrixStack.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MelonBombEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
