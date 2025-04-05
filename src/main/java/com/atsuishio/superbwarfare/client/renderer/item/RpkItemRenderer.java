package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.RpkItemModel;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.machinegun.RpkItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
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

public class RpkItemRenderer extends GeoItemRenderer<RpkItem> {

    public RpkItemRenderer() {
        super(new RpkItemModel());
        // TODO layer
// this.addRenderLayer(new RpkLayer(this));
    }

    @Override
    public RenderType getRenderType(RpkItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;
    public ItemDisplayContext transformType;
    protected RpkItem animatable;
    private final Set<String> hiddenBones = new HashSet<>();

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int p_239207_6_) {
        this.transformType = transformType;
        if (this.animatable != null)
            this.animatable.getTransformType(transformType);
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, p_239207_6_);
    }

    @Override
    public void actuallyRender(PoseStack matrixStackIn, RpkItem animatable, BakedGeoModel model, RenderType type, MultiBufferSource renderTypeBuffer, VertexConsumer vertexBuilder, boolean isRenderer, float partialTicks, int packedLightIn,
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
    public void renderRecursively(PoseStack stack, RpkItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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
        var tag = GunData.from(itemStack).tag();

        if (name.equals("holo")) {
            bone.setHidden(tag.getBoolean("HoloHidden") || !ClientEventHandler.zoom);
        }
        if (name.equals("Cross1")) {
            bone.setHidden(tag.getBoolean("HoloHidden")
                    || !ClientEventHandler.zoom
                    || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) != 1);
        }

        if (name.equals("Cross2")) {
            bone.setHidden(tag.getBoolean("HoloHidden")
                    || !ClientEventHandler.zoom
                    || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) != 2
                    || tag.getBoolean("ScopeAlt"));
        }

        if (name.equals("CrossAlt")) {
            bone.setHidden(tag.getBoolean("HoloHidden")
                    || !ClientEventHandler.zoom
                    || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) != 2
                    || !(tag.getBoolean("ScopeAlt")));
        }

        if (name.equals("Cross3")) {
            bone.setHidden(tag.getBoolean("HoloHidden")
                    || !ClientEventHandler.zoom
                    || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) != 3);
        }

        if (name.equals("humu1")) {
            bone.setHidden(GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.GRIP) != 0);
        }

        if (name.equals("humu2")) {
            bone.setHidden(GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.GRIP) == 0);
        }

        if (GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 2 && !tag.getBoolean("ScopeAlt")
                && (name.equals("glass") || name.equals("Barrel") || name.equals("humu") || name.equals("qiangguan"))) {
            bone.setHidden(!tag.getBoolean("HoloHidden") && ClientEventHandler.zoom);
        }

        if (GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.SCOPE) == 3
                && (name.equals("jing") || name.equals("Barrel") || name.equals("humu") || name.equals("qiangguan") || name.equals("houzhunxing"))) {
            bone.setHidden(!tag.getBoolean("HoloHidden") && ClientEventHandler.zoom);
        }

        if (name.equals("flare")) {
            if (ClientEventHandler.firePosTimer == 0 || ClientEventHandler.firePosTimer > 0.5 || GunsTool.getAttachmentType(itemStack, GunsTool.AttachmentType.BARREL) == 2) {
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
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, false, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(RpkItem instance) {
        return super.getTextureLocation(instance);
    }
}
