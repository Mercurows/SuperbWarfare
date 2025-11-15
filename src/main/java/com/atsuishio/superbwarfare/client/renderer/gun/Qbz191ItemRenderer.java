package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.model.item.Qbz191ItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.rifle.Qbz191Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class Qbz191ItemRenderer extends CustomGunRenderer<Qbz191Item> {

    public Qbz191ItemRenderer() {
        super(new Qbz191ItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, Qbz191Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red,
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

        boolean needHide = name.equals("Tuoxin");
        if (itemStack.getItem() instanceof GunItem && GeoItem.getId(itemStack) == this.getInstanceId(animatable)) {
            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {

                var data = GunData.from(itemStack);
                if (needHide) {
                    bone.setHidden(data.attachment.get(AttachmentType.STOCK) == 0);
                }

                AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn);
                ItemModelHelper.handleGunAttachments(bone, itemStack, name);

                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    if (data.attachment.get(AttachmentType.SCOPE) == 2 && bone.getName().endsWith("_hide2")) {
                        bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                    }
                    if (data.attachment.get(AttachmentType.SCOPE) == 3 && bone.getName().endsWith("_hide3")) {
                        bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                    }

                    int scopeType = data.attachment.get(AttachmentType.SCOPE);

                    switch (scopeType) {
                        case 1 ->
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.27, 20, 1.2f, 255, 0, 0, 255, "dot", false);
                        case 2 ->
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.28, 13, 1.5f, 255, 0, 0, 255, "qmk", true);
                        case 3 ->
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.275, 27, 0.35f * (float) ClientEventHandler.customZoom, 255, 0, 0, 255, "lpvo", true);
                    }
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
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}
