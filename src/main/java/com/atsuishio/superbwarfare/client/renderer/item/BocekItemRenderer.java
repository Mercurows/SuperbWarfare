package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.CustomRenderer;
import com.atsuishio.superbwarfare.client.ModRenderTypes;
import com.atsuishio.superbwarfare.client.model.item.BocekItemModel;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtil;

import java.util.HashSet;
import java.util.Set;

public class BocekItemRenderer extends CustomRenderer<BocekItem> {

    public BocekItemRenderer() {
        super(new BocekItemModel());
    }

    @Override
    public RenderType getRenderType(BocekItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected BocekItem animatable;
    private final Set<String> hiddenBones = new HashSet<>();

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
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        if (this.animatable != null)
            this.animatable.getTransformType(transformType);
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, BocekItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, BocekItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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

        if (name.equals("arrow")) {
            var data = GunData.from(itemStack);
            bone.setHidden(data.ammo.get() == 0);
        }

        if (name.equals("arrow2")) {
            var data = GunData.from(itemStack);
            bone.setHidden(data.ammo.get() != 0);
        }

        AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0.002, 0.1790625, 0.13, 0.08f, 255, 0, 0, 255, "dot", false);

        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, false, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }


    @Override
    public ResourceLocation getTextureLocation(BocekItem instance) {
        return super.getTextureLocation(instance);
    }
}
