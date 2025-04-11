package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.model.item.InsidiousItemModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.item.gun.rifle.InsidiousItem;
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

public class InsidiousItemRenderer extends GeoItemRenderer<InsidiousItem> {

    public InsidiousItemRenderer() {
        super(new InsidiousItemModel());
        // TODO layer
//        this.addRenderLayer(new InsidiousLayer(this));
    }

    @Override
    public RenderType getRenderType(InsidiousItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected InsidiousItem animatable;
    private final Set<String> hiddenBones = new HashSet<>();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        if (this.animatable != null)
            this.animatable.getTransformType(transformType);
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, InsidiousItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, InsidiousItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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
        if (!itemStack.is(ModTags.Items.GUN)) return;

        if (name.equals("flare")) {
            if (ClientEventHandler.firePosTimer == 0 || ClientEventHandler.firePosTimer > 0.5 || GunData.from(itemStack).attachment.get(AttachmentType.BARREL) == 2) {
                bone.setHidden(true);
            } else {
                bone.setHidden(false);
                bone.setScaleX((float) (0.95 + 0.5 * (Math.random() - 0.5)));
                bone.setScaleY((float) (0.95 + 0.5 * (Math.random() - 0.5)));
                bone.setRotZ((float) (0.5 * (Math.random() - 0.5)));
            }
        }

        if (name.equals("rot1")) {
            bone.setRotZ((System.currentTimeMillis() % 36000000) / 400f);
        }

        if (name.equals("rot2")) {
            bone.setRotZ(-(System.currentTimeMillis() % 36000000) / 400f);
        }

        if (name.equals("cross")) {
            bone.setHidden(ClientEventHandler.zoomPos < 0.7 || !ClientEventHandler.zoom);
        }

        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, false, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(InsidiousItem instance) {
        return super.getTextureLocation(instance);
    }
}
