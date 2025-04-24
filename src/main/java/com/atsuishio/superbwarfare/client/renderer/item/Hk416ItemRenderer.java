package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.Hk416ItemModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.item.gun.rifle.Hk416Item;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.HashSet;
import java.util.Set;

public class Hk416ItemRenderer extends GeoItemRenderer<Hk416Item> {
    public static double zoomFactor;

    public Hk416ItemRenderer() {
        super(new Hk416ItemModel());
    }

    @Override
    public RenderType getRenderType(Hk416Item animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected Hk416Item animatable;
    private final Set<String> hiddenBones = new HashSet<>();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        if (this.animatable != null)
            this.animatable.getTransformType(transformType);
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, Hk416Item animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, Hk416Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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

        if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2
                && (name.equals("hidden"))) {
            bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
        }

        if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3
                && (name.equals("jing") || name.equals("Barrel") || name.equals("yugu") || name.equals("qiangguan"))) {
            bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
        }

        int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);
        zoomFactor = Mth.lerp(0.01 * partialTick, zoomFactor, GunsTool.getGunDoubleTag(GunData.from(itemStack).tag, "CustomZoom"));

        switch (scopeType) {
            case 1 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, packedLightIn, 0, 0.25, 30, 0, 255, 0, 255, "eotech");
            case 2 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, packedLightIn, 0, 0.313, 9, 255, 0, 0, 255, "acog");
            case 3 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, packedLightIn, 0, 0.29, Math.max(38 - 5.2 * zoomFactor, 3), 255, 0, 0, 255, "lpvo");
        }
        ;

        AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0, 1.440625, 0.3);

        ItemModelHelper.handleGunAttachments(bone, itemStack, name);

        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, false, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(Hk416Item instance) {
        return super.getTextureLocation(instance);
    }
}
