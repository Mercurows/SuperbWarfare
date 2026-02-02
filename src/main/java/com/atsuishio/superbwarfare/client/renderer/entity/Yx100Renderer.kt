package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Yx100GlowLayer
import com.atsuishio.superbwarfare.client.model.entity.Yx100Model
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel

class Yx100Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Yx100Entity>(renderManager, Yx100Model()) {

    init {
        this.addRenderLayer(Yx100GlowLayer(this))
    }

    override fun preRender(
        poseStack: PoseStack,
        entity: Yx100Entity,
        model: BakedGeoModel,
        bufferSource: MultiBufferSource?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        val scale = 1.25f
        this.scaleHeight = scale
        this.scaleWidth = scale
        super.preRender(
            poseStack,
            entity,
            model,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            color
        )
    }
}
