package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.model.item.Ql1031ItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.sniper.Ql1031Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class Ql1031ItemRenderer extends CustomGunRenderer<Ql1031Item> {
    public Ql1031ItemRenderer() {
        super(new Ql1031ItemModel());
    }

    public static float progress;

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
            var data = GunData.from(itemStack);

            if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                if (name.equals("Sight")) {
                    bone.setHidden(data.attachment.get(AttachmentType.SCOPE) == 3);
                }

                AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn);
                ItemModelHelper.handleGunAttachments(bone, itemStack, name);

                var energy = itemStack.getCapability(ForgeCapabilities.ENERGY)
                        .map(IEnergyStorage::getEnergyStored)
                        .orElse(0);

                if (name.equals("energy2_illuminated") || name.equals("energy3_illuminated") || name.equals("energy4_illuminated")) {
                    bone.setScaleX((float) energy / data.get(GunProp.MAX_ENERGY));
                    bone.setHidden(ClientEventHandler.zoomPos < 0.7);
                }

                if (name.equals("energy_illuminated")) {
                    bone.setScaleX((float) energy / data.get(GunProp.MAX_ENERGY));
                }

                if (name.equals("kuang_illuminated") || name.equals("kuang2_illuminated") || name.equals("kuang3_illuminated")) {
                    bone.setHidden(ClientEventHandler.zoomPos < 0.7);
                }

                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    if (data.attachment.get(AttachmentType.SCOPE) == 2 && !itemStack.getOrCreateTag().getBoolean("ScopeAlt") && (name.equals("hidden") || name.equals("qianzhunxingzu"))) {
                        bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                    }
                    if (data.attachment.get(AttachmentType.SCOPE) == 3 && (bone.getName().endsWith("_hide3") || name.equals("qianzhunxingzu") || name.equals("Barrel"))) {
                        bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
                    }

                    int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);
                    progress = Mth.lerp(partialTick, progress, ClientEventHandler.holdingFireKeyTicks);

                    switch (scopeType) {
                        case 0 -> {
                            AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.264, 0.85, 0.06f, 255, 0, 0, 255, "dot", false);
                            AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.264, 0.85, 0.05f, 255, 255, 255, 255, "ql_front", false);
                            stack.pushPose();
                            float spread = (float) (ClientEventHandler.gunSpread + 1 * ClientEventHandler.boltMove);
                            int gb = 255 - (int) (data.heat.get() * 2.55);
                            stack.rotateAround(Axis.ZP.rotationDegrees(45), 0, 0.264f, 0);
                            AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.264, -0.2, 0.025f + 0.003f * spread, 255, gb, gb, 255, "ql_back", false);
                            stack.popPose();
                            renderHoldProgress(stack, name, bone, buffer, 0, 0.264f, -0.2, 0.04f);
                        }

                        case 1 -> {
                            AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.32, 30, 1.2f, 255, 0, 0, 255, "dot", false);
                            renderHoldProgress(stack, name, bone, buffer, 0, 0.307f, -0.12, 0.051f);
                        }

                        case 2 -> {
                            if (itemStack.getOrCreateTag().getBoolean("ScopeAlt")) {
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.34, 30, 0.25f, 255, 0, 0, 255, "delta", false);
                                renderHoldProgress(stack, name, bone, buffer, 0, 0.353f, -0.12, 0.015f);
                            } else {
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.278, 13, 0.87f, 255, 0, 0, 255, "hamr", true);
                                renderHoldProgress(stack, name, bone, buffer, 0, 0.281f, -0.12, 0.048f);
                            }
                        }
                        case 3 -> {
                                AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.283, 27, 2f, 255, 0, 0, 255, "sniper", true);
                            renderHoldProgress(stack, name, bone, buffer, 0, 0.29, 27, 1.6f);
                        }
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

    public void renderHoldProgress(PoseStack stack, String name, GeoBone bone, MultiBufferSource buffer,double x, double y, double z, float size) {
        int c = 255 - (int) (progress * 12.75);
        for (int i = 0; i < (9f / 2) * progress; i += 3) {
            stack.pushPose();
            stack.rotateAround(Axis.ZP.rotationDegrees(-i), 0F, (float) y, 0);
            AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, x, y, z, size, 255, c, c, 255, "ql_hold_progress", false);
            stack.popPose();
        }
        for (int i = 3; i < (9f / 2) * progress; i += 3) {
            stack.pushPose();
            stack.rotateAround(Axis.ZP.rotationDegrees(i), 0, (float) y, 0);
            AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, x, y, z, size, 255, c, c, 255, "ql_hold_progress", false);
            stack.popPose();
        }
    }
}
