package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.layer.gun.SvdLayer;
import com.atsuishio.superbwarfare.client.model.item.SvdItemModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.sniper.SvdItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashSet;
import java.util.Set;

public class SvdItemRenderer extends GeoItemRenderer<SvdItem> {

    public SvdItemRenderer() {
        super(new SvdItemModel());
        this.addRenderLayer(new SvdLayer(this));
    }

    @Override
    public RenderType getRenderType(SvdItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected SvdItem animatable;
    private final Set<String> hiddenBones = new HashSet<>();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        if (this.animatable != null)
            this.animatable.getTransformType(transformType);
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, SvdItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, SvdItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        boolean renderingArms = false;
        if (name.equals("Lefthand") || name.equals("Righthand")) {
            bone.setHidden(true);
            renderingArms = true;
        } else {
            bone.setHidden(this.hiddenBones.contains(name));
        }

        Player player = mc.player;
        if (player == null) return;
        ItemStack itemStack = player.getMainHandItem();
        if (!itemStack.is(ModTags.Items.GUN)) return;

        if (name.equals("mount")) {
            bone.setHidden(GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 0 || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 2);
        }

        if (name.equals("Cross1")) {
            bone.setHidden(NBTTool.getOrCreateTag(itemStack).getBoolean("HoloHidden")
                    || !ClientEventHandler.zoom
                    || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) != 1);
        }

        if (name.equals("Cross2")) {
            bone.setHidden(NBTTool.getOrCreateTag(itemStack).getBoolean("HoloHidden")
                    || !ClientEventHandler.zoom
                    || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) != 2);
        }

        if ((GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 2 || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 3)
                && (name.equals("Hidden2") || name.equals("Hidden") || name.equals("gun") || name.equals("bolt") || name.equals("Lefthand") || name.equals("Barrel") || name.equals("bipod") || name.equals("mount")) && ClientEventHandler.zoom && !NBTTool.getOrCreateTag(itemStack).getBoolean("HoloHidden")) {
            bone.setHidden(true);
            renderingArms = false;
        }

        if (name.equals("flare")) {
            if (ClientEventHandler.firePosTimer == 0 || ClientEventHandler.firePosTimer > 0.5 || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.BARREL) == 2 || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.BARREL) == 3) {
                bone.setHidden(true);
            } else {
                if (GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 2 && ClientEventHandler.zoom && !NBTTool.getOrCreateTag(itemStack).getBoolean("HoloHidden")) {
                    bone.setPosY(-2f);
                }
                bone.setHidden(false);
                bone.setScaleX((float) (0.75 + 0.5 * (Math.random() - 0.5)));
                bone.setScaleY((float) (0.75 + 0.5 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            }
        }

        // TODO handle gun attachments
//            ItemModelHelper.handleGunAttachments(bone, itemStack, name);

        if (this.transformType.firstPerson() && renderingArms) {
            AbstractClientPlayer localPlayer = mc.player;

            if (localPlayer == null) {
                return;
            }

            PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(localPlayer);
            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
            stack.pushPose();
            RenderUtil.translateMatrixToBone(stack, bone);
            RenderUtil.translateToPivotPoint(stack, bone);
            RenderUtil.rotateMatrixAroundBone(stack, bone);
            RenderUtil.scaleMatrixForBone(stack, bone);
            RenderUtil.translateAwayFromPivotPoint(stack, bone);
            ResourceLocation loc = localPlayer.getSkin().texture();
            VertexConsumer armBuilder = this.currentBuffer.getBuffer(RenderType.entitySolid(loc));
            VertexConsumer sleeveBuilder = this.currentBuffer.getBuffer(RenderType.entityTranslucent(loc));
            if (name.equals("Lefthand")) {
                stack.translate(-1.0f * SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);
                AnimationHelper.renderPartOverBone2(model.leftArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
                AnimationHelper.renderPartOverBone2(model.leftSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
            } else {
                stack.translate(SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);
                AnimationHelper.renderPartOverBone2(model.rightArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
                AnimationHelper.renderPartOverBone2(model.rightSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
            }

            this.currentBuffer.getBuffer(this.renderType);
            stack.popPose();
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(SvdItem instance) {
        return super.getTextureLocation(instance);
    }
}