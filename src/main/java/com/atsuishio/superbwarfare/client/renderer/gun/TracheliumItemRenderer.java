package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.TracheliumItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.handgun.Trachelium;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class TracheliumItemRenderer extends CustomGunRenderer<Trachelium> {

    public TracheliumItemRenderer() {
        super(new TracheliumItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, Trachelium animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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
        if (!(itemStack.getItem() instanceof GunItem)) return;

        if (name.equals("humu")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 0 && GunData.from(itemStack).attachment.get(AttachmentType.GRIP) == 0);
        }

        if (name.equals("qianzhunxing1")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) > 0 || GunData.from(itemStack).attachment.get(AttachmentType.GRIP) > 0);
        }

        if (name.equals("railup")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 0);
        }

        if (name.equals("raildown")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.GRIP) == 0);
        }


        if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2 && !NBTTool.getTag(itemStack).getBoolean("ScopeAlt") && (name.equals("hidden"))) {
            bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
        }

        int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);

        switch (scopeType) {
            case 1 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.3, 30, 1.2f, 255, 0, 0, 255, "dot", false);
            case 2 -> {
                if (NBTTool.getTag(itemStack).getBoolean("ScopeAlt")) {
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.36, 30, 0.18f, 255, 0, 0, 255, "delta", false);
                } else {
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.294, 13, 0.87f, 255, 0, 0, 255, "hamr", true);
                }
            }
        }

        ItemModelHelper.handleGunAttachments(bone, itemStack, name);

        AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0, 0.734375, 0.5);

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.transformType, stack, name, bone, this.currentBuffer, type, packedLightIn, true);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }
}