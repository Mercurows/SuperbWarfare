package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.BocekItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.special.BocekItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

public class BocekItemRenderer extends CustomGunRenderer<BocekItem> {

    public BocekItemRenderer() {
        super(new BocekItemModel());
    }

    @Override
    public void illuminatedRender(PoseStack poseStack, BocekItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
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

        if (bone.getName().equals("power_light")) {
            var power = Math.round((float) ClientEventHandler.bowPower * 255);
            var c = FastColor.ARGB32.color(color & 0xFF, power, power, power);
            renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(ModRenderTypes.ILLUMINATED.apply(this.getTextureLocation(animatable))),
                    packedLight, OverlayTexture.NO_OVERLAY, c);
        }
        this.illuminatedRenderChildBones(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay, color);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack stack, BocekItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        boolean renderingArms = false;
        if (name.equals("Lefthand") || name.equals("Righthand")) {
            bone.setHidden(true);
            renderingArms = true;
        } else {
            bone.setHidden(false);
        }

        var player = mc.player;
        if (player == null) return;

        ItemStack itemStack = player.getMainHandItem();

        boolean needHide = name.equals("safang");

        if (itemStack.getItem() instanceof GunItem && GeoItem.getId(itemStack) == this.getInstanceId(animatable)) {
            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                ItemModelHelper.handleGunAttachments(bone, itemStack, name);

                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    if (name.equals("arrow")) {
                        var data = GunData.from(itemStack);
                        bone.setHidden(data.ammo.get() == 0);
                    }
                    if (name.equals("arrow2")) {
                        var data = GunData.from(itemStack);
                        bone.setHidden(data.ammo.get() != 0);
                    }

                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0.002, 0.1790625, 0.13, 0.08f, 255, 0, 0, 255, "dot", false);
                } else if (needHide) {
                    bone.setHidden(true);
                }

            } else {
                ItemModelHelper.hideAllAttachments(bone, name);
                if (needHide) {
                    bone.setHidden(true);
                }
            }
        } else {
            ItemModelHelper.hideAllAttachments(bone, name);
            if (needHide) {
                bone.setHidden(true);
            }
        }

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.renderPerspective, stack, name, bone, buffer, type, packedLightIn, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }
}
