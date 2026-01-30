package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.model.item.SentinelItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.sniper.SentinelItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class SentinelItemRenderer extends CustomGunRenderer<SentinelItem> {

    public SentinelItemRenderer() {
        super(new SentinelItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, SentinelItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red,
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

        boolean needHide = name.equals("wires") || name.equals("charge_illuminated");

        if (itemStack.getItem() instanceof GunItem && GeoItem.getId(itemStack) == this.getInstanceId(animatable)) {
            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {

                boolean[] flag = {false};
                itemStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                        iEnergyStorage -> flag[0] = iEnergyStorage.getEnergyStored() > 0
                );

                if (name.equals("charge_illuminated")) {
                    bone.setHidden(!flag[0]);
                    bone.setRotZ((System.currentTimeMillis() % 36000000) / 200f);
                }

                if (name.equals("charge_illuminated")) {
                    bone.setHidden(!flag[0]);
                    bone.setRotZ((System.currentTimeMillis() % 36000000) / 200f);
                }

                if (name.equals("chamber2")) {
                    bone.setRotZ((float) (bone.getRotZ() + 5 * ClientEventHandler.recoilForce));
                }

                ItemModelHelper.handleGunAttachments(bone, itemStack, name);
                AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn);

                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.265, -0.05, 0.075f, 255, 0, 0, 255, "apex_3x", false);
                } else {
                    if (name.equals("wires")) {
                        bone.setHidden(true);
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
            AnimationHelper.renderArms(player, this.renderPerspective, stack, name, bone, buffer, type, packedLightIn, true);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}
