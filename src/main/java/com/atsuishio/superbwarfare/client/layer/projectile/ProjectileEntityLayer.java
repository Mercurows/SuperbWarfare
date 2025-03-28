package com.atsuishio.superbwarfare.client.layer.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ProjectileEntityLayer extends GeoRenderLayer<ProjectileEntity> {
    private static final ResourceLocation LAYER = Mod.loc("textures/entity/projectile_entity.png");

    public ProjectileEntityLayer(GeoRenderer<ProjectileEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, ProjectileEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.eyes(LAYER);

        float r = animatable.getEntityData().get(ProjectileEntity.COLOR_R);
        float g = animatable.getEntityData().get(ProjectileEntity.COLOR_G);
        float b = animatable.getEntityData().get(ProjectileEntity.COLOR_B);

        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType,
                bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color((int) (0.8 * 255), (int) (r * 255), (int) (g * 255), (int) (b * 255)));
    }
}
