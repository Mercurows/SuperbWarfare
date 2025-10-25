package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.model.item.Ql1031ItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.sniper.Ql1031Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class Ql1031ItemRenderer extends CustomGunRenderer<Ql1031Item> {

    public Ql1031ItemRenderer() {
        super(new Ql1031ItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, Ql1031Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red,
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
            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                if (name.equals("Sight")) {
                    bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3);
                }

                AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn);
                ItemModelHelper.handleGunAttachments(bone, itemStack, name);

                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2 && !itemStack.getOrCreateTag().getBoolean("ScopeAlt") && (name.equals("hidden") || name.equals("qianzhunxingzu"))) {
                        bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                    }
                    if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 3 && (bone.getName().endsWith("_hide3") || name.equals("qianzhunxingzu") || name.equals("Barrel"))) {
                        bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                    }

                    int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);

                    switch (scopeType) {
                        case 1 ->
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.32, 30, 1.2f, 255, 0, 0, 255, "dot", false);
                        case 2 -> {
                            if (itemStack.getOrCreateTag().getBoolean("ScopeAlt")) {
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.34, 30, 0.25f, 255, 0, 0, 255, "delta", false);
                            } else {
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.278, 13, 0.87f, 255, 0, 0, 255, "hamr", true);
                            }
                        }
                        case 3 ->
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.283, 27, 2f, 255, 0, 0, 255, "sniper", true);
                    }
                }

            } else {
                ItemModelHelper.hideAllAttachments(bone, name);
            }
        } else {
            ItemModelHelper.hideAllAttachments(bone, name);
        }

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.renderPerspective, stack, name, bone, buffer, type, packedLightIn, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}
