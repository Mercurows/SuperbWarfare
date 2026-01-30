package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.T90ALayer
import com.atsuishio.superbwarfare.client.model.entity.T90aModel
import com.atsuishio.superbwarfare.entity.vehicle.T90aEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel

class T90aRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<T90aEntity>(renderManager, T90aModel()) {
    init {
        this.addRenderLayer(T90ALayer(this))
    }

    override fun preRender(
        poseStack: PoseStack?,
        entity: T90aEntity?,
        model: BakedGeoModel?,
        bufferSource: MultiBufferSource?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        val scale = 1.1f
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
