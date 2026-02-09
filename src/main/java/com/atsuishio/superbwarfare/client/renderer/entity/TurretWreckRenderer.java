package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TurretWreckModel;
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TurretWreckRenderer extends GeoEntityRenderer<TurretWreckEntity> {
    public TurretWreckRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TurretWreckModel());
        this.shadowRadius = 1f;
    }

    @Override
    public RenderType getRenderType(TurretWreckEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, TurretWreckEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        // TODO 测试用，用好了给这个删了
        var type = EntityType.byString("superbwarfare:lav_150");
        if (type.isEmpty()) return;
        var vehicle = type.get().create(animatable.level());
        if (vehicle == null) return;
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(vehicle);
        if (vehicle instanceof GeoAnimatable geoAnimatable && renderer instanceof GeoEntityRenderer geoEntityRenderer) {
            var model = geoEntityRenderer.getGeoModel();
            var bakedModel = model.getBakedModel(model.getModelResource(geoAnimatable));
            var optionalBone = bakedModel.getBone("turret");
            if (optionalBone.isEmpty()) return;
            var tBone = optionalBone.get();
            var source = bufferSource.getBuffer(RenderType.entityTranslucent(model.getTextureResource(geoAnimatable)));
            geoEntityRenderer.renderChildBones(poseStack, geoAnimatable, tBone, renderType, bufferSource, source, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    @Override
    public void render(TurretWreckEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityIn.getYaw(partialTicks)));
        poseStack.mulPose(Axis.XP.rotationDegrees(entityIn.getPitch(partialTicks)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entityIn.getRoll(partialTicks)));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }
}
