package com.atsuishio.superbwarfare.client.renderer.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;

public class StickWoodenRenderer extends BlockEntityWithoutLevelRenderer {

    public StickWoodenRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher, EntityModelSet entityModelSet) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    private static final ItemStack STICK_ITEM = new ItemStack(Items.STICK);

    @Override
    @ParametersAreNonnullByDefault
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (Math.random() > 0.8) {
            poseStack.pushPose();

            poseStack.translate(0.5, 0.5, 0.5);

//        poseStack.translate((float) Math.random(),  (float) Math.random(),  (float) Math.random());
//        poseStack.scale((float) Math.random(), (float) Math.random(), (float) Math.random());
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));

            RenderSystem.setShaderColor(1, 1, 1, 0.2f);
            Minecraft.getInstance().getItemRenderer()
                    .renderStatic(STICK_ITEM, displayContext, packedLight, packedOverlay, poseStack, buffer, null, 0);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            poseStack.popPose();
        }

        super.renderByItem(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
    }
}
