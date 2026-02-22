package com.atsuishio.superbwarfare.client.layer.misc

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.TargetEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer

class TargetLayer(entityRenderer: GeoRenderer<TargetEntity>) : GeoRenderLayer<TargetEntity>(entityRenderer) {
    override fun render(
        poseStack: PoseStack?,
        animatable: TargetEntity?,
        bakedModel: BakedGeoModel?,
        renderType: RenderType?,
        bufferSource: MultiBufferSource,
        buffer: VertexConsumer?,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val glowRenderType = RenderType.eyes(LAYER)
        getRenderer().reRender(
            getDefaultBakedModel(animatable),
            poseStack,
            bufferSource,
            animatable,
            glowRenderType,
            bufferSource.getBuffer(glowRenderType),
            partialTick,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            1f,
            1f,
            1f,
            1f
        )
    }

    companion object {
        private val LAYER = loc("textures/entity/target_e.png")
    }
}
