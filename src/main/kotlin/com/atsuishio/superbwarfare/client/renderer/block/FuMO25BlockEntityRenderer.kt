package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.client.model.block.FuMO25Model
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoBlockRenderer

class FuMO25BlockEntityRenderer : GeoBlockRenderer<FuMO25BlockEntity>(FuMO25Model()) {
    override fun getRenderType(
        animatable: FuMO25BlockEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    override fun shouldRenderOffScreen(pBlockEntity: FuMO25BlockEntity): Boolean {
        return false
    }

    override fun getViewDistance(): Int {
        return 512
    }
}
