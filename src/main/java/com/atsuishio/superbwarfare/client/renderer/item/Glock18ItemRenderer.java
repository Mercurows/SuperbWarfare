package com.atsuishio.superbwarfare.client.renderer.item;

import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.CustomRenderer;
import com.atsuishio.superbwarfare.client.model.item.Glock18ItemModel;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.handgun.Glock18Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;

public class Glock18ItemRenderer extends CustomRenderer<Glock18Item> {

    public Glock18ItemRenderer() {
        super(new Glock18ItemModel());
    }

    @Override
    public RenderType getRenderType(Glock18Item animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    private static final float SCALE_RECIPROCAL = 1.0f / 16.0f;

    @Override
    public void renderRecursively(PoseStack stack, Glock18Item animatable, GeoBone bone, RenderType type, MultiBufferSource buffer, VertexConsumer bufferIn, boolean isReRender, float partialTick, int packedLightIn, int packedOverlayIn, int color) {
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

        AnimationHelper.handleShootFlare(name, stack, itemStack, bone, buffer, packedLightIn, 0, 0, 0.3022, 0.3);

        if (renderingArms) {
            AnimationHelper.renderArms(mc, player, this.transformType, stack, name, bone, SCALE_RECIPROCAL, this.currentBuffer, type, packedLightIn, false, false);
        }
        super.renderRecursively(stack, animatable, bone, type, buffer, bufferIn, isReRender, partialTick, packedLightIn, packedOverlayIn, color);
    }

    @Override
    public ResourceLocation getTextureLocation(Glock18Item instance) {
        return super.getTextureLocation(instance);
    }
}
