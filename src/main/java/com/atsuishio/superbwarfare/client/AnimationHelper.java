package com.atsuishio.superbwarfare.client;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

public class AnimationHelper {

    public static void renderPartOverBone(ModelPart model, GeoBone bone, PoseStack stack, VertexConsumer buffer, int packedLightIn, int packedOverlayIn) {
        setupModelFromBone(model, bone);
        model.render(stack, buffer, packedLightIn, packedOverlayIn);
    }

    public static void setupModelFromBone(ModelPart model, GeoBone bone) {
        model.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
        model.xRot = 0.0f;
        model.yRot = 0.0f;
        model.zRot = 0.0f;
    }

    public static void renderPartOverBoneR(ModelPart model, GeoBone bone, PoseStack stack, VertexConsumer buffer, int packedLightIn, int packedOverlayIn) {
        renderPartOverBone(model, bone, stack, buffer, packedLightIn, packedOverlayIn);
    }

    public static void renderPartOverBone2(ModelPart model, GeoBone bone, PoseStack stack, VertexConsumer buffer, int packedLightIn, int packedOverlayIn) {
        setupModelFromBone2(model, bone);
        model.render(stack, buffer, packedLightIn, packedOverlayIn);
    }

    public static void setupModelFromBone2(ModelPart model, GeoBone bone) {
        model.setPos(bone.getPivotX(), bone.getPivotY() + 7, bone.getPivotZ());
        model.xRot = 0.0f;
        model.yRot = 180 * Mth.DEG_TO_RAD;
        model.zRot = 180 * Mth.DEG_TO_RAD;
    }

    public static void renderPartOverBone2R(ModelPart model, GeoBone bone, PoseStack stack, VertexConsumer buffer, int packedLightIn, int packedOverlayIn) {
        setupModelFromBone2R(model, bone);
        model.render(stack, buffer, packedLightIn, packedOverlayIn);
    }

    public static void setupModelFromBone2R(ModelPart model, GeoBone bone) {
        model.setPos(bone.getPivotX(), bone.getPivotY() + 7, bone.getPivotZ());
        model.xRot = 180 * Mth.DEG_TO_RAD;
        model.yRot = 180 * Mth.DEG_TO_RAD;
        model.zRot = 0;
    }

    public static void handleShellsAnimation(AnimationProcessor<?> animationProcessor, float x, float y) {
        GeoBone shell1 = animationProcessor.getBone("shell1");
        GeoBone shell2 = animationProcessor.getBone("shell2");
        GeoBone shell3 = animationProcessor.getBone("shell3");
        GeoBone shell4 = animationProcessor.getBone("shell4");
        GeoBone shell5 = animationProcessor.getBone("shell5");

        ClientEventHandler.handleShells(x, y, shell1, shell2, shell3, shell4, shell5);
    }

    public static void handleReloadShakeAnimation(ItemStack stack, GeoBone main, GeoBone camera, float roll, float pitch) {
        var data = GunData.from(stack);
        if (data.reload.time() > 0) {
            main.setRotX(roll * main.getRotX());
            main.setRotY(roll * main.getRotY());
            main.setRotZ(roll * main.getRotZ());
            main.setPosX(pitch * main.getPosX());
            main.setPosY(pitch * main.getPosY());
            main.setPosZ(pitch * main.getPosZ());
            camera.setRotX(roll * camera.getRotX());
            camera.setRotY(roll * camera.getRotY());
            camera.setRotZ(roll * camera.getRotZ());
        }
    }


    public static void handleShootFlare(String name, PoseStack stack, ItemStack itemStack, GeoBone bone, MultiBufferSource buffer, int packedLightIn, double x, double y, double z, double size) {
        if (name.equals("flare") && ClientEventHandler.firePosTimer > 0 && ClientEventHandler.firePosTimer < 0.5 && GunData.from(itemStack).attachment.get(AttachmentType.BARREL) != 2) {
            bone.setScaleX((float) (size + 0.8 * size * (Math.random() - 0.5)));
            bone.setScaleY((float) (size + 0.8 * size * (Math.random() - 0.5)));
            bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));

            float height = 0f;

            if ((GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2 || GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3) && ClientEventHandler.zoom) {
                height = -0.07f;
            }

            stack.pushPose();
            stack.translate(x, y + 0.02 + height, -z);
            RenderUtil.translateMatrixToBone(stack, bone);
            RenderUtil.translateToPivotPoint(stack, bone);
            RenderUtil.rotateMatrixAroundBone(stack, bone);
            RenderUtil.scaleMatrixForBone(stack, bone);
            RenderUtil.translateAwayFromPivotPoint(stack, bone);
            PoseStack.Pose pose = stack.last();
            VertexConsumer vertexConsumer = buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(Mod.loc("textures/particle/flare.png")));
            vertex(vertexConsumer, pose, packedLightIn, 0.0F, 0, 0, 1);
            vertex(vertexConsumer, pose, packedLightIn, 1.0F, 0, 1, 1);
            vertex(vertexConsumer, pose, packedLightIn, 1.0F, 1, 1, 0);
            vertex(vertexConsumer, pose, packedLightIn, 0.0F, 1, 0, 0);
            stack.popPose();
        }
    }

    private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pPose, int pLightmapUV, float pX, float pY, int pU, int pV) {
        pConsumer.addVertex(pPose, pX - 0.5F, pY - 0.5F, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0.0F, 1.0F, 0.0F);
    }

    public static void handleZoomCrossHair(MultiBufferSource currentBuffer, RenderType renderType, String boneName, PoseStack stack, GeoBone bone, MultiBufferSource buffer, double x, double y, double z, float size, int r, int g, int b, int a, String name, boolean hasBlackPart) {
        if (boneName.equals("cross") && ClientEventHandler.zoomPos > 0.8) {
            stack.pushPose();
            stack.translate(x, y, -z);
            RenderUtil.translateMatrixToBone(stack, bone);
            RenderUtil.translateToPivotPoint(stack, bone);
            RenderUtil.rotateMatrixAroundBone(stack, bone);
            RenderUtil.scaleMatrixForBone(stack, bone);
            RenderUtil.translateAwayFromPivotPoint(stack, bone);
            PoseStack.Pose pose = stack.last();
            Matrix4f $$7 = pose.pose();

            ResourceLocation tex = Mod.loc("textures/crosshair/" + name + ".png");

            int alpha = hasBlackPart ? a : (int) (0.12 * a);

            VertexConsumer blackPart = buffer.getBuffer(RenderType.entityTranslucent(tex));
            vertexRGB(blackPart, $$7, pose, 255, 0.0F, 0, 0, 1, r, g, b, alpha, size);
            vertexRGB(blackPart, $$7, pose, 255, size, 0, 1, 1, r, g, b, alpha, size);
            vertexRGB(blackPart, $$7, pose, 255, size, size, 1, 0, r, g, b, alpha, size);
            vertexRGB(blackPart, $$7, pose, 255, 0.0F, size, 0, 0, r, g, b, alpha, size);

            VertexConsumer $$9 = buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(tex));
            vertexRGB($$9, $$7, pose, 255, 0.0F, 0, 0, 1, r, g, b, a, size);
            vertexRGB($$9, $$7, pose, 255, size, 0, 1, 1, r, g, b, a, size);
            vertexRGB($$9, $$7, pose, 255, size, size, 1, 0, r, g, b, a, size);
            vertexRGB($$9, $$7, pose, 255, 0.0F, size, 0, 0, r, g, b, a, size);

            stack.popPose();
        }
        currentBuffer.getBuffer(renderType);
    }

    private static void vertexRGB(VertexConsumer pConsumer, Matrix4f pPose, PoseStack.Pose pNormal, int pLightmapUV, float pX, float pY, int pU, int pV, int r, int g, int b, int a, float size) {
        pConsumer.addVertex(pPose, pX - 0.5F * size, pY - 0.5F * size, 0.0F)
                .setColor(r, g, b, a)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pNormal, 0.0F, 1.0F, 0.0F);
    }

    public static final float SCALE_RECIPROCAL = 1 / 16.0f;

    public static void renderArms(LocalPlayer localPlayer, ItemDisplayContext transformType, PoseStack stack, String name, GeoBone bone,
                                  MultiBufferSource currentBuffer, RenderType renderType, int packedLightIn, boolean useOldHandRender) {
        if (transformType != null && transformType.firstPerson()) {
            var mc = Minecraft.getInstance();
            PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(localPlayer);
            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
            stack.pushPose();
            RenderUtil.translateMatrixToBone(stack, bone);
            RenderUtil.translateToPivotPoint(stack, bone);
            RenderUtil.rotateMatrixAroundBone(stack, bone);
            RenderUtil.scaleMatrixForBone(stack, bone);
            RenderUtil.translateAwayFromPivotPoint(stack, bone);
            ResourceLocation loc = localPlayer.getSkin().texture();
            if (name.equals("Lefthand")) {
                if (!model.leftArm.visible) {
                    model.leftArm.visible = true;
                }
                if (!model.leftSleeve.visible && mc.options.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE)) {
                    model.leftSleeve.visible = true;
                }

                stack.translate(-1.0f * CustomGunRenderer.SCALE_RECIPROCAL, 2.0f * CustomGunRenderer.SCALE_RECIPROCAL, 0.0f);
                if (useOldHandRender) {
                    AnimationHelper.renderPartOverBone(model.leftArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                    AnimationHelper.renderPartOverBone(model.leftSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                } else {
                    AnimationHelper.renderPartOverBone2(model.leftArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                    AnimationHelper.renderPartOverBone2(model.leftSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                }
            } else {
                if (!model.rightArm.visible) {
                    model.rightArm.visible = true;
                }
                if (!model.rightSleeve.visible && mc.options.isModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE)) {
                    model.rightSleeve.visible = true;
                }

                stack.translate(CustomGunRenderer.SCALE_RECIPROCAL, 2.0f * CustomGunRenderer.SCALE_RECIPROCAL, 0.0f);
                if (useOldHandRender) {
                    AnimationHelper.renderPartOverBoneR(model.leftArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                    AnimationHelper.renderPartOverBoneR(model.leftSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                } else {
                    AnimationHelper.renderPartOverBone2R(model.leftArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                    AnimationHelper.renderPartOverBone2R(model.leftSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, OverlayTexture.NO_OVERLAY);
                }
            }
            currentBuffer.getBuffer(renderType);
            stack.popPose();
        }
    }
}
