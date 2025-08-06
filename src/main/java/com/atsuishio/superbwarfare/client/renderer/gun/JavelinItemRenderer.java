package com.atsuishio.superbwarfare.client.renderer.gun;

import com.atsuishio.superbwarfare.client.model.item.JavelinItemModel;
import com.atsuishio.superbwarfare.client.renderer.CustomGunRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.launcher.JavelinItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.cache.object.GeoBone;

public class JavelinItemRenderer extends CustomGunRenderer<JavelinItem> {

    public JavelinItemRenderer() {
        super(new JavelinItemModel());
    }

    @Override
    public void renderRecursively(PoseStack stack, JavelinItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, float red,
                                  float green, float blue, float alpha) {
        Minecraft mc = Minecraft.getInstance();
        String name = bone.getName();
        var player = mc.player;
        if (player == null) return;

        ItemStack itemStack = player.getMainHandItem();

        if (itemStack.getItem() instanceof GunItem && GeoItem.getId(itemStack) == this.getInstanceId(animatable)) {
            if (name.equals("javelin")) {
                if (this.renderPerspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
                    double zp = ClientEventHandler.zoomPos;
                    bone.setHidden(zp > 0.8);
                } else {
                    bone.setHidden(false);
                }
            }
        }

        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}
