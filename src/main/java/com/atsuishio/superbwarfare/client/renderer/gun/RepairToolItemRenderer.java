package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.model.item.RepairToolItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.special.RepairToolItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class RepairToolItemRenderer extends CustomGunRenderer<RepairToolItem> {

    public RepairToolItemRenderer() {
        super(new RepairToolItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, RepairToolItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red,
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
                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    for (int i = 0; i < 8; i++) {
                        if (name.equals("flame_" + i + "_illuminated")) {
                            bone.setHidden(i != player.tickCount % 8);
                        }
                    }
                }
            } else {
                ItemModelHelper.hideAllAttachments(bone, name);
                if (name.equals("flame_illuminated")) {
                    bone.setHidden(true);
                }
            }
        } else {
            if (name.equals("flame_illuminated")) {
                bone.setHidden(true);
            }
        }

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.renderPerspective, stack, name, bone, buffer, type, packedLightIn, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}

