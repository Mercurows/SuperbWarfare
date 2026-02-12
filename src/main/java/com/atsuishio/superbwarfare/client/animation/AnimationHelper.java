package com.atsuishio.superbwarfare.client.animation;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.RenderPlayerArmEvent;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes;
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.resource.gun.GunResource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.activeThermalImaging;

public class AnimationHelper {

    public static float lerpTimer;

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


    public static void handleShootFlare(String name, PoseStack stack, ItemStack itemStack, GeoBone bone, MultiBufferSource buffer, int packedLightIn) {
        if (!(itemStack.getItem() instanceof GunItem)) return;

        var gunResource = GunResource.from(itemStack).compute();
        if (gunResource.flarePosition != null) {
            handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, gunResource.flarePosition.x, gunResource.flarePosition.y, gunResource.flarePosition.z, gunResource.flareSize);
        }
    }

    public static void handleShootFlare(String name, PoseStack stack, ItemStack itemStack, GeoBone bone, MultiBufferSource buffer, int packedLightIn, double x, double y, double z, double size) {
        var data = GunData.from(itemStack);

        if (name.equals("flare") && ClientEventHandler.fireRotTimer > 0 && ClientEventHandler.fireRotTimer < 0.3 && data.attachment.get(AttachmentType.BARREL) != 2) {
            bone.setScaleX((float) (size + 0.8 * size * (Math.random() - 0.5)));
            bone.setScaleY((float) (size + 0.8 * size * (Math.random() - 0.5)));
            bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));

            float height = 0f;

            if ((data.attachment.get(AttachmentType.SCOPE) == 2 || data.attachment.get(AttachmentType.SCOPE) == 3) && ClientEventHandler.zoom) {
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
            vertex(vertexConsumer, pose, packedLightIn, 0, 0, 0, 1);
            vertex(vertexConsumer, pose, packedLightIn, 1, 0, 1, 1);
            vertex(vertexConsumer, pose, packedLightIn, 1, 1, 1, 0);
            vertex(vertexConsumer, pose, packedLightIn, 0, 1, 0, 0);
            stack.popPose();

            lerpTimer = Mth.lerp(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), lerpTimer, (float) ClientEventHandler.fireRotTimer * 0.667f);

//            handleShootSmoke(stack, bone, buffer, packedLightIn, x, y, z, height);
//            handleShootSmoke2(stack, bone, buffer, packedLightIn, x, y, z, height);
        }
    }

    public static void handleShootSmoke(PoseStack stack, GeoBone bone, MultiBufferSource buffer, int packedLightIn, double x, double y, double z, double height) {
        stack.pushPose();
        stack.translate(x, y + height - 0.03, -z);
        RenderUtil.translateMatrixToBone(stack, bone);
        RenderUtil.translateToPivotPoint(stack, bone);
        RenderUtil.rotateMatrixAroundBone(stack, bone);
        RenderUtil.scaleMatrixForBone(stack, bone);
        RenderUtil.translateAwayFromPivotPoint(stack, bone);
        PoseStack.Pose $$6 = stack.last();

        stack.scale(3f + lerpTimer * 20f, 3f + lerpTimer * 20f, 1);

        VertexConsumer $$9 = buffer.getBuffer(RenderType.entityTranslucent(Mod.loc("textures/particle/shoot_smoke.png")));
        vertexSmoke($$9, $$6, packedLightIn, 0 - 0.15f - lerpTimer, 0, 0, 1, lerpTimer);
        vertexSmoke($$9, $$6, packedLightIn, 1 - 0.15f - lerpTimer, 0, 1, 1, lerpTimer);
        vertexSmoke($$9, $$6, packedLightIn, 1 - 0.15f - lerpTimer, 1, 1, 0, lerpTimer);
        vertexSmoke($$9, $$6, packedLightIn, 0 - 0.15f - lerpTimer, 1, 0, 0, lerpTimer);

        stack.popPose();
    }

    public static void handleShootSmoke2(PoseStack stack, GeoBone bone, MultiBufferSource buffer, int packedLightIn, double x, double y, double z, double height) {
        stack.pushPose();
        stack.translate(x, y + height - 0.03, -z);
        RenderUtil.translateMatrixToBone(stack, bone);
        RenderUtil.translateToPivotPoint(stack, bone);
        RenderUtil.rotateMatrixAroundBone(stack, bone);
        RenderUtil.scaleMatrixForBone(stack, bone);
        RenderUtil.translateAwayFromPivotPoint(stack, bone);
        PoseStack.Pose $$6 = stack.last();

        stack.scale(3f + lerpTimer * 20f, 3f + lerpTimer * 20f, 1);

        VertexConsumer $$9 = buffer.getBuffer(RenderType.entityTranslucentEmissive(Mod.loc("textures/particle/shoot_smoke2.png")));
        vertexSmoke($$9, $$6, packedLightIn, 0 + 0.15f + lerpTimer, 0, 0, 1, lerpTimer);
        vertexSmoke($$9, $$6, packedLightIn, 1 + 0.15f + lerpTimer, 0, 1, 1, lerpTimer);
        vertexSmoke($$9, $$6, packedLightIn, 1 + 0.15f + lerpTimer, 1, 1, 0, lerpTimer);
        vertexSmoke($$9, $$6, packedLightIn, 0 + 0.15f + lerpTimer, 1, 0, 0, lerpTimer);

        stack.popPose();
    }

    private static void vertexSmoke(VertexConsumer pConsumer, PoseStack.Pose pPose, int pLightmapUV, float pX, float pY, int pU, int pV, double time) {
        pConsumer.addVertex(pPose, pX - 0.5F, pY - 0.5F, 0)
                .setColor(255, 255, 255, (int) (96 - 40 * time))
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0F, 1F, 0F);
    }

    private static void vertex(VertexConsumer pConsumer, PoseStack.Pose pPose, int pLightmapUV, float pX, float pY, int pU, int pV) {
        pConsumer.addVertex(pPose, pX - 0.5F, pY - 0.5F, 0)
                .setColor(255, 255, 255, 255)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pPose, 0, 1, 0);
    }

    public static void handleZoomCrossHair(MultiBufferSource currentBuffer, RenderType renderType, String boneName, PoseStack stack, GeoBone bone, MultiBufferSource buffer, double x, double y, double z, float size, int r, int g, int b, int a, String name, boolean hasBlackPart) {
        if (boneName.equals("cross") && ClientEventHandler.zoomPos > 0.1) {
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

            a = (int) (3 * Mth.clamp(ClientEventHandler.zoomTime - 0.34, 0, 1) * 255);

            int alpha = hasBlackPart ? a : (int) (0.12 * a);

            if (activeThermalImaging) {
                r = 255;
                g = 255;
                b = 255;
                a = 255;
                tex = SmartTextureBrightener.getSmartBrightenedTexture(tex, 10);
            }

            VertexConsumer blackPart = buffer.getBuffer(RenderType.entityTranslucentEmissive(tex));
            vertexRGB(blackPart, $$7, pose, 255, 0, 0, 0, 1, r, g, b, alpha, size);
            vertexRGB(blackPart, $$7, pose, 255, size, 0, 1, 1, r, g, b, alpha, size);
            vertexRGB(blackPart, $$7, pose, 255, size, size, 1, 0, r, g, b, alpha, size);
            vertexRGB(blackPart, $$7, pose, 255, 0, size, 0, 0, r, g, b, alpha, size);

            VertexConsumer $$9 = buffer.getBuffer(ModRenderTypes.MUZZLE_FLASH_TYPE.apply(tex));
            vertexRGB($$9, $$7, pose, 255, 0, 0, 0, 1, r, g, b, a, size);
            vertexRGB($$9, $$7, pose, 255, size, 0, 1, 1, r, g, b, a, size);
            vertexRGB($$9, $$7, pose, 255, size, size, 1, 0, r, g, b, a, size);
            vertexRGB($$9, $$7, pose, 255, 0, size, 0, 0, r, g, b, a, size);

            stack.popPose();
        }
        currentBuffer.getBuffer(renderType);
    }

    private static void vertexRGB(VertexConsumer pConsumer, Matrix4f pPose, PoseStack.Pose pNormal, int pLightmapUV, float pX, float pY, int pU, int pV, int r, int g, int b, int a, float size) {
        pConsumer.addVertex(pPose, pX - 0.5F * size, pY - 0.5F * size, 0)
                .setColor(r, g, b, a)
                .setUv((float) pU, (float) pV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(pLightmapUV)
                .setNormal(pNormal, 0, 1, 0);
    }

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

            HumanoidArm arm = "Lefthand".equals(name) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
            var renderPlayerArmEvent = new RenderPlayerArmEvent(localPlayer, transformType, stack, arm, bone, currentBuffer, renderType, packedLightIn, useOldHandRender);
            if (NeoForge.EVENT_BUS.post(renderPlayerArmEvent).isCanceled()) {
                currentBuffer.getBuffer(renderType); // 用来重置 Render Type，防止后续渲染出错
                stack.popPose();
                return;
            }

            ResourceLocation loc = localPlayer.getSkin().texture();
            int overlayTexture = activeThermalImaging ? OverlayTexture.pack(15, 10) : OverlayTexture.NO_OVERLAY;

            if (activeThermalImaging) {
                packedLightIn = LightTexture.FULL_BRIGHT;
            }

            if (arm == HumanoidArm.LEFT) {
                if (!model.leftArm.visible) {
                    model.leftArm.visible = true;
                }
                if (!model.leftSleeve.visible && mc.options.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE)) {
                    model.leftSleeve.visible = true;
                }

                stack.translate(-1.0f * CustomGunRenderer.SCALE_RECIPROCAL, 2.0f * CustomGunRenderer.SCALE_RECIPROCAL, 0.0f);
                if (useOldHandRender) {
                    AnimationHelper.renderPartOverBone(model.leftArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, overlayTexture);
                    AnimationHelper.renderPartOverBone(model.leftSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, overlayTexture);
                } else {
                    AnimationHelper.renderPartOverBone2(model.leftArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, overlayTexture);
                    AnimationHelper.renderPartOverBone2(model.leftSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, overlayTexture);
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
                    AnimationHelper.renderPartOverBone(model.rightArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, overlayTexture);
                    AnimationHelper.renderPartOverBone(model.rightSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, overlayTexture);
                } else {
                    AnimationHelper.renderPartOverBone2(model.rightArm, bone, stack, currentBuffer.getBuffer(RenderType.entitySolid(loc)), packedLightIn, overlayTexture);
                    AnimationHelper.renderPartOverBone2(model.rightSleeve, bone, stack, currentBuffer.getBuffer(RenderType.entityTranslucent(loc)), packedLightIn, overlayTexture);
                }
            }

            currentBuffer.getBuffer(renderType); // 用来重置 Render Type，防止后续渲染出错
            stack.popPose();
        }
    }
}
