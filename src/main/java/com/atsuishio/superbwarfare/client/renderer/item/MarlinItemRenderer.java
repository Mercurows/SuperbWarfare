package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.CustomRenderer;
import com.atsuishio.superbwarfare.client.model.item.MarlinItemModel;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.rifle.MarlinItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class MarlinItemRenderer extends CustomRenderer<MarlinItem> {

    public MarlinItemRenderer() {
        super(new MarlinItemModel());
    }

    @Override
    public RenderType getRenderType(MarlinItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;

    @Override
    public void renderRecursively(PoseStack stack, MarlinItem animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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

        AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0, 1.33720625, 0.4);

        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, true, true);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(MarlinItem instance) {
        return super.getTextureLocation(instance);
    }
}
