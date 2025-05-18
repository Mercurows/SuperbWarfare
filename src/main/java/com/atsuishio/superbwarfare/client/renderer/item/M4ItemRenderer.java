package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.CustomRenderer;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.M4ItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.rifle.M4Item;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class M4ItemRenderer extends CustomRenderer<M4Item> {

    public M4ItemRenderer() {
        super(new M4ItemModel());
    }

    @Override
    public RenderType getRenderType(M4Item animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;

    @Override
    public void renderRecursively(PoseStack stack, M4Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        boolean renderingArms = false;
        if (name.equals("Lefthand") || name.equals("Righthand")) {
            bone.setHidden(true);
            renderingArms = true;
        }

        var player = mc.player;
        if (player == null) return;
        ItemStack itemStack = player.getMainHandItem();
        if (!(itemStack.getItem() instanceof GunItem)) return;

        if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2 && !NBTTool.getTag(itemStack).getBoolean("ScopeAlt") && (name.equals("hidden"))) {
            bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
        }

        if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3
                && (name.equals("hidden2") || name.equals("yugu") || name.equals("qiangguan") || name.equals("Barrel"))) {
            bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
        }

        int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);

        switch (scopeType) {
            case 1 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.32, 30, 1.2f, 255, 0, 0, 255, "dot", false);
            case 2 -> {
                if (NBTTool.getTag(itemStack).getBoolean("ScopeAlt")) {
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.34, 30, 0.25f, 255, 0, 0, 255, "delta", false);
                } else {
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.294, 13, 0.87f, 255, 0, 0, 255, "hamr", true);
                }
            }
            case 3 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.283, 27, 2f, 255, 0, 0, 255, "sniper", true);
        }

        AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0, 1.353125, 0.3);

        if (name.equals("Sight")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3);
        }

        ItemModelHelper.handleGunAttachments(bone, itemStack, name);

        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, false, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(M4Item instance) {
        return super.getTextureLocation(instance);
    }
}
