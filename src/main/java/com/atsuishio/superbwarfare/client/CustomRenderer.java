package com.atsuishio.superbwarfare.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class CustomRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {

    public CustomRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());

            bone.setModelSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(RenderUtil.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations));
        }

        poseStack.pushPose();
        RenderUtil.prepMatrixForBone(poseStack, bone);

        // TODO 为什么会出现全部发光？
        // 我感觉这个地方已经查明了对于指定后缀名group的全部cube，并且传入一个魔改后的buffer来实现发光渲染，后续用的buffer都是之前的内容，理论上不会影响递归调用啊
        var bf = buffer;
        var overlay = packedOverlay;
        if (bone.getName().endsWith("_illuminated")) {
            bf = bufferSource.getBuffer(ModRenderTypes.GUN_ILLUMINATED.apply(this.getTextureLocation(animatable)));
            overlay = OverlayTexture.NO_OVERLAY;
        }

        renderCubesOfBone(poseStack, bone, bf, packedLight, overlay, color);

        if (!isReRender) {
            applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        }

        renderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
