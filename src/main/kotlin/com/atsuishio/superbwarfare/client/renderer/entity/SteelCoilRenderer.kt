package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.SteelCoilModel
import com.atsuishio.superbwarfare.entity.living.SteelCoilEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import software.bernie.geckolib.renderer.GeoEntityRenderer

class SteelCoilRenderer(renderManager: EntityRendererProvider.Context) : GeoEntityRenderer<SteelCoilEntity>(
    renderManager,
    SteelCoilModel()
) {
    init {
        this.scaleWidth = 2f
        this.scaleHeight = 2f
    }

    override fun getRenderType(
        animatable: SteelCoilEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType = RenderType.entityTranslucent(getTextureLocation(animatable))

    override fun getDeathMaxRotation(entityLivingBaseIn: SteelCoilEntity): Float {
        return 0f
    }

    override fun shouldShowName(animatable: SteelCoilEntity): Boolean {
        return animatable.hasCustomName()
    }

    @Suppress("removal")
    override fun getPackedOverlay(animatable: SteelCoilEntity?, u: Float): Int {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(false))
    }
}