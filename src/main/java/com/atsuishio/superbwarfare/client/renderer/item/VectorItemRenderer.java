package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.VectorItemModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.item.gun.smg.VectorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.HashSet;
import java.util.Set;

public class VectorItemRenderer extends GeoItemRenderer<VectorItem> {

    private static final float SCALE_RECIPROCAL = 0.0625f;
    protected boolean renderArms;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected VectorItem animatable;
    private final Set<String> hiddenBones;


    public VectorItemRenderer() {
        super(new VectorItemModel());
        // TODO layer

// this.addRenderLayer(new VectorLayer(this));

        this.renderArms = false;
        this.hiddenBones = new HashSet<>();
    }

    public RenderType getRenderType(VectorItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }


    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, VectorItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, VectorItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        boolean renderingArms = false;
        if (name.equals("Lefthand") || name.equals("Righthand")) {
            bone.setHidden(true);
            renderingArms = true;
        } else {
            bone.setHidden(this.hiddenBones.contains(name));
        }

        var player = mc.player;
        if (player == null) return;
        ItemStack itemStack = player.getMainHandItem();
        if (!(itemStack.getItem() instanceof GunItem)) return;

        var data = GunData.from(itemStack);

        if (name.equals("Cross1")) {
            bone.setHidden(ClientEventHandler.zoomPos < 0.7 || data.attachment.get(AttachmentType.SCOPE) != 1);
        }

        if (name.equals("Cross2")) {
            bone.setHidden(ClientEventHandler.zoomPos < 0.7 || data.attachment.get(AttachmentType.SCOPE) != 2);
        }

        if (name.equals("tuoxin")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.STOCK) == 0);
        }

        if (name.equals("flare")) {
            if (ClientEventHandler.firePosTimer == 0 || ClientEventHandler.firePosTimer > 0.5 || GunData.from(itemStack).attachment.get(AttachmentType.BARREL) == 2) {
                bone.setHidden(true);
            } else {
                bone.setHidden(false);
                bone.setScaleX((float) (0.55 + 0.5 * (Math.random() - 0.5)));
                bone.setScaleY((float) (0.55 + 0.5 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            }
        }

        ItemModelHelper.handleGunAttachments(bone, itemStack, name);


        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, true, true);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }


//    @Override
//    public void renderRecursively(PoseStack stack, VectorItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
//        Minecraft mc = Minecraft.getInstance();
//        String name = bone.getName();
//        boolean renderingArms = false;
//        if (name.equals("Lefthand") || name.equals("Righthand")) {
//            bone.setHidden(true);
//            renderingArms = true;
//        } else {
//            bone.setHidden(this.hiddenBones.contains(name));
//        }
//
//        Player player = mc.player;
//        if (player == null) return;
//        ItemStack itemStack = player.getMainHandItem();
//        if (!(itemStack.getItem() instanceof GunItem)) return;
//
//        if (name.equals("Cross1")) {
//            bone.setHidden(ClientEventHandler.zoomPos < 0.7
//                    || !ClientEventHandler.zoom
//                    || GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) != 1);
//        }
//
//        if (name.equals("Cross2")) {
//            bone.setHidden(ClientEventHandler.zoomPos < 0.7
//                    || !ClientEventHandler.zoom
//                    || GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) != 2);
//        }
//
//        if (name.equals("tuoxin")) {
//            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.STOCK) == 0);
//        }
//
//        if (name.equals("flare")) {
//            if (ClientEventHandler.firePosTimer == 0 || Clientdom() - 0.5)));
////                bone.setScaleY((float) (0.55 + 0.5 * (Math.random() - 0.5)));
////                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
////            }
////        }
////
////        ItemModelHelper.handleGunAttachments(bone, itemStack, name);EventHandler.firePosTimer > 0.5 || GunData.from(itemStack).attachment.get(AttachmentType.BARREL) == 2) {
//                bone.setHidden(true);
//            } else {
//                bone.setHidden(false);
//                bone.setScaleX((float) (0.55 + 0.5 * (Math.ran
//
////        type.
//        if (this.transformType.firstPerson() && renderingArms) {
//            AbstractClientPlayer localPlayer = mc.player;
//
//            if (localPlayer == null) {
//                return;
//            }
//
//            PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(localPlayer);
//            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
//            stack.pushPose();
//
//            RenderUtil.translateMatrixToBone(stack, bone);
//            RenderUtil.translateToPivotPoint(stack, bone);
//            RenderUtil.rotateMatrixAroundBone(stack, bone);
//            RenderUtil.scaleMatrixForBone(stack, bone);
//            RenderUtil.translateAwayFromPivotPoint(stack, bone);
//            ResourceLocation loc = localPlayer.getSkin().texture();
//            VertexConsumer armBuilder = this.currentBuffer.getBuffer(RenderType.entitySolid(loc));
//            VertexConsumer sleeveBuilder = this.currentBuffer.getBuffer(RenderType.entityTranslucent(loc));
//            if (name.equals("Lefthand")) {
//                stack.translate(-1.0f * SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);
//                AnimationHelper.renderPartOverBone(model.leftArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
//                AnimationHelper.renderPartOverBone(model.leftSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
//            } else {
//                stack.translate(SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);
//                AnimationHelper.renderPartOverBone(model.rightArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
//                AnimationHelper.renderPartOverBone(model.rightSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
//            }
//
//            this.currentBuffer.getBuffer(this.renderType);
//            stack.popPose();
//        }
//        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
//    }

}
