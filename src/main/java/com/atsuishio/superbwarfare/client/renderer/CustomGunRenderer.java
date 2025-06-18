package com.atsuishio.superbwarfare.client.renderer;

import com.atsuishio.superbwarfare.client.model.item.CustomGunModel;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.RenderUtil;

public class CustomGunRenderer<T extends GunItem & GeoAnimatable> extends GeoItemRenderer<T> {

    public static final float SCALE_RECIPROCAL = 1.0f / 16.0f;

//    public static final int LOD_DISTANCE = 100;

    protected T animatable;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;

    public CustomGunRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, T animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
                               int packedOverlayIn, int color) {
        this.currentBuffer = renderTypeBuffer;
        this.renderType = type;
        this.animatable = animatable;
        super.actuallyRender(matrixStackIn, animatable, model, type, renderTypeBuffer, vertexBuilder, isRenderer, partialTicks, packedLightIn, packedOverlayIn, color);
        if (this.renderArms) {
            this.renderArms = false;
        }
    }

    @Override
    public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        var geoModel = getGeoModel();

        if (renderPerspective != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && DisplayConfig.ENABLE_GUN_LOD.get()
                && geoModel instanceof CustomGunModel<T> gunModel
        ) {
            return gunModel.getLODTextureResource(animatable);
        }

        return geoModel.getTextureResource(animatable, null);
    }

//    public ResourceLocation getTextureLocation(T animatable, PoseStack poseStack) {
//        var geoModel = getGeoModel();
//
//        if (renderPerspective != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
//                && DisplayConfig.ENABLE_GUN_LOD.get()
//                && geoModel instanceof CustomGunModel<T> gunModel
//        ) {
//            var player = Minecraft.getInstance().player;
//            if (player != null) {
//                Vec3 pos = new Vec3(poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32());
//                if (pos.lengthSqr() >= LOD_DISTANCE) {
//                    return gunModel.getLODTextureResource(animatable);
//                } else {
//                    return geoModel.getTextureResource(animatable);
//                }
//            }
//            return gunModel.getLODTextureResource(animatable);
//        }
//        return geoModel.getTextureResource(animatable);
//    }

//    @Override
//    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
//        this.animatable = (T) stack.getItem();
//        this.currentItemStack = stack;
//        this.renderPerspective = transformType;
//
//        if (transformType == ItemDisplayContext.GUI) {
//            renderInGui(transformType, poseStack, bufferSource, packedLight, packedOverlay);
//        } else {
//            RenderType renderType = getRenderType(this.animatable, getTextureLocation(this.animatable, poseStack), bufferSource, Minecraft.getInstance().getFrameTime());
//            VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, false, this.currentItemStack != null && this.currentItemStack.hasFoil());
//
//            defaultRender(poseStack, this.animatable, bufferSource, renderType, buffer,
//                    0, Minecraft.getInstance().getFrameTime(), packedLight);
//        }
//    }

    @Override
    public void defaultRender(PoseStack poseStack, T animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();

        Color renderColor = getRenderColor(animatable, partialTick, packedLight);
        int packedOverlay = getPackedOverlay(animatable, 0, partialTick);

//        var player = Minecraft.getInstance().player;

        ResourceLocation modelLocation;
        var geoModel = getGeoModel();
        if (renderPerspective != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && DisplayConfig.ENABLE_GUN_LOD.get()
                && geoModel instanceof CustomGunModel<T> gunModel
        ) {
//            if (player != null) {
//                Vec3 pos = new Vec3(poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32());
//                if (pos.lengthSqr() >= LOD_DISTANCE) {
//                    modelLocation = gunModel.getLODModelResource(animatable);
//                } else {
            // TODO 这个地方有问题，如果是在这里使用了高模，会导致custom animation无法分离
//                    modelLocation = geoModel.getModelResource(animatable);
//                }
//            } else {
            modelLocation = gunModel.getLODModelResource(animatable);
//            }
        } else {
            modelLocation = geoModel.getModelResource(animatable, null);
        }

        BakedGeoModel model = geoModel.getBakedModel(modelLocation);

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
