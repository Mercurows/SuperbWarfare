package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.ItemModelHelper;
import com.atsuishio.superbwarfare.client.model.item.Mp5ItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.smg.Mp5Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class Mp5ItemRenderer extends CustomGunRenderer<Mp5Item> {

    public Mp5ItemRenderer() {
        super(new Mp5ItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, Mp5Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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

        if (name.equals("yugu")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.GRIP) == 0);
        }

        if (name.equals("bashou")) {
            bone.setHidden(GunData.from(itemStack).attachment.get(AttachmentType.GRIP) != 0);
        }

        if (GunData.from(itemStack).attachment.get(AttachmentType.SCOPE) == 2 && (bone.getName().endsWith("_hide2"))) {
            bone.setHidden(ClientEventHandler.zoomPos > 0.7 && ClientEventHandler.zoom);
        }

        int scopeType = GunData.from(itemStack).attachment.get(AttachmentType.SCOPE);
        switch (scopeType) {
            case 1 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.42, 14, 1, 255, 0, 0, 255, "dot", false);
            case 2 ->
                    AnimationHelper.handleZoomCrossHair(currentBuffer, renderType, name, stack, bone, buffer, 0, 0.52, 14, 1.4f, 255, 0, 0, 255, "acog", true);
        }

        AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0.05, 1, 0.35);

        ItemModelHelper.handleGunAttachments(bone, itemStack, name);

        if (renderingArms) {
            AnimationHelper.renderArms(player, this.transformType, stack, name, bone, this.currentBuffer, type, packedLightIn, false);
        }

        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }
}
