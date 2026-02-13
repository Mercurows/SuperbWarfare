package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.Bmp2Layer
import com.atsuishio.superbwarfare.client.model.entity.Bmp2Model
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity
import com.atsuishio.superbwarfare.entity.vehicle.T90aEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import software.bernie.geckolib.cache.`object`.BakedGeoModel

class Bmp2Renderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<Bmp2Entity>(renderManager, Bmp2Model()) {

    init {
        this.addRenderLayer(Bmp2Layer(this))
    }

    override fun preRender(poseStack: PoseStack?, entity: Bmp2Entity?, model: BakedGeoModel?, bufferSource: MultiBufferSource?, buffer: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, red: Float, green: Float,
                           blue: Float, alpha: Float) {
        val scale = 0.9f
        this.scaleHeight = scale
        this.scaleWidth = scale
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha)
    }
}
