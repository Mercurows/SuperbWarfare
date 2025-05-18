package com.atsuishio.superbwarfare.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.RenderUtil;

public class CustomRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {

    public CustomRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void defaultRender(PoseStack poseStack, T animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();

        Color renderColor = getRenderColor(animatable, partialTick, packedLight);
        float red = renderColor.getRedFloat();
        float green = renderColor.getGreenFloat();
        float blue = renderColor.getBlueFloat();
        float alpha = renderColor.getAlphaFloat();
        int packedOverlay = getPackedOverlay(animatable, 0, partialTick);
        BakedGeoModel model = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable));

        if (renderType == null)
            renderType = getRenderType(animatable, getTextureLocation(animatable), bufferSource, partialTick);

        if (buffer == null)
            buffer = bufferSource.getBuffer(renderType);

        preRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor.argbInt());

        if (firePreRenderEvent(poseStack, model, bufferSource, partialTick, packedLight)) {
            preApplyRenderLayers(poseStack, animatable, model, renderType, bufferSource, buffer, packedLight, packedLight, packedOverlay);
            actuallyRender(poseStack, animatable, model, renderType,
                    bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor.argbInt());
            this.renderIlluminatedBones(model, poseStack, bufferSource, animatable, renderType, buffer, partialTick, packedLight, packedOverlay, renderColor.argbInt());
            postRender(poseStack, animatable, model, bufferSource, buffer, false, partialTick, packedLight, packedOverlay, renderColor.argbInt());
            firePostRenderEvent(poseStack, model, bufferSource, partialTick, packedLight);
        }

        poseStack.popPose();

        renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, renderColor.argbInt());
    }

    public void renderIlluminatedBones(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable,
                                       RenderType renderType, VertexConsumer buffer, float partialTick,
                                       int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        preRender(poseStack, animatable, model, bufferSource, buffer, true, partialTick, packedLight, packedOverlay, color);

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());

        updateAnimatedTextureFrame(animatable);

        for (GeoBone bone : model.topLevelBones()) {
            this.illuminatedRender(poseStack, animatable, bone, renderType, bufferSource, buffer,
                    partialTick, packedLight, packedOverlay, color);
        }

        postRender(poseStack, animatable, model, bufferSource, buffer, true, partialTick, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    public void illuminatedRender(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                                  int packedOverlay, int color) {
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());

            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        poseStack.pushPose();
        RenderUtil.prepMatrixForBone(poseStack, bone);

        if (bone.getName().endsWith("_illuminated")) {
            renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(ModRenderTypes.ILLUMINATED.apply(this.getTextureLocation(animatable))),
                    packedLight, OverlayTexture.NO_OVERLAY, color);
        }
        this.illuminatedRenderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    public void illuminatedRenderChildBones(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                            float partialTick, int packedLight, int packedOverlay, int color) {
        if (bone.isHidingChildren())
            return;

        for (GeoBone childBone : bone.getChildBones()) {
            illuminatedRender(poseStack, animatable, childBone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay, color);
        }
    }
}
