package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.decorator.ContainerItemDecorator
import com.atsuishio.superbwarfare.client.model.item.ContainerItemModel
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import software.bernie.geckolib.renderer.GeoItemRenderer

class ContainerBlockItemRenderer : GeoItemRenderer<ContainerBlockItem>(ContainerItemModel()) {

    override fun getRenderType(
        animatable: ContainerBlockItem,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    override fun renderByItem(
        stack: ItemStack,
        transformType: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        // GUI模式下有自定义图标时，隐藏3D集装箱模型，由ContainerItemDecorator绘制2D图标
        if (transformType == ItemDisplayContext.GUI && ContainerItemDecorator.getCustomIcon(stack) != null) {
            return
        }
        super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay)
    }
}
