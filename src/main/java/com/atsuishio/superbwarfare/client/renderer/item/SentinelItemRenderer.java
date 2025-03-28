package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.layer.gun.SentinelLayer;
import com.atsuishio.superbwarfare.client.layer.gun.SentinelLightLayer;
import com.atsuishio.superbwarfare.client.model.item.SentinelItemModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.sniper.SentinelItem;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashSet;
import java.util.Set;

public class SentinelItemRenderer extends GeoItemRenderer<SentinelItem> {

    public SentinelItemRenderer() {
        super(new SentinelItemModel());
        this.addRenderLayer(new SentinelLayer(this));
        this.addRenderLayer(new SentinelLightLayer(this));
    }

    @Override
    public RenderType getRenderType(SentinelItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected SentinelItem animatable;
    private final Set<String> hiddenBones = new HashSet<>();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        if (this.animatable != null)
            this.animatable.getTransformType(transformType);
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, SentinelItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, SentinelItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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

        if (name.equals("flare")) {
            if (ClientEventHandler.firePosTimer == 0 || ClientEventHandler.firePosTimer > 0.5) {
                bone.setHidden(true);
            } else {
                bone.setHidden(false);
                bone.setScaleX((float) (0.75 + 0.5 * (Math.random() - 0.5)));
                bone.setScaleY((float) (0.75 + 0.5 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            }
        }

        if (name.equals("holo")) {
            bone.setHidden(NBTTool.getTag(itemStack).getBoolean("HoloHidden") || !ClientEventHandler.zoom);
        }

        var cap = itemStack.getCapability(Capabilities.EnergyStorage.ITEM);
        var flag = cap != null && cap.getEnergyStored() > 0;

        if (name.equals("charge")) {
            bone.setHidden(!flag);
        }

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
                AnimationHelper.renderPartOverBone(model.leftArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
                AnimationHelper.renderPartOverBone(model.leftSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
            } else {
                stack.translate(SCALE_RECIPROCAL, 2.0f * SCALE_RECIPROCAL, 0.0f);
                AnimationHelper.renderPartOverBone(model.rightArm, bone, stack, armBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
                AnimationHelper.renderPartOverBone(model.rightSleeve, bone, stack, sleeveBuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1);
            }

            this.currentBuffer.getBuffer(this.renderType);
            stack.popPose();
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(SentinelItem instance) {
        return super.getTextureLocation(instance);
    }
}
