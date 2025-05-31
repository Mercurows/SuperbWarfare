package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.Mk14ItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.rifle.Mk14Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class Mk14ItemRenderer extends CustomGunRenderer<Mk14Item> {

    public Mk14ItemRenderer() {
        super(new Mk14ItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, Mk14Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red,
                                  float green, float blue, float alpha) {
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
        if (itemStack.getItem() instanceof GunItem && GeoItem.getId(itemStack) == this.getInstanceId(animatable)) {
            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                if (name.equals("qiaojia")) {
                    bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 0);
                }
                if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2
                        && (name.equals("hidden"))) {
                    bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                }
                if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3
                        && (name.equals("jing") || name.equals("yugu") || name.equals("qiangguan") || name.equals("Barrel"))) {
                    bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                }
            }

            int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);

            switch (scopeType) {
                case 1 ->
                        AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.25, 30, 0.8f, 255, 0, 0, 255, "eotech", false);
                case 2 ->
                        AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.313, 9, 1, 255, 0, 0, 255, "acog", true);
                case 3 ->
                        AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.29, 45, (float) ClientEventHandler.customZoom, 255, 0, 0, 255, "lpvo", true);
            }

            AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0, 1.475, 0.33);
            ItemModelHelper.handleGunAttachments(bone, itemStack, name);
        } else {
            ItemModelHelper.hideAllAttachments(bone, name);
            if (name.equals("qiaojia")) {
                bone.setHidden(true);
            }
        }

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.renderPerspective, stack, name, bone, buffer, type, packedLightIn, true);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}