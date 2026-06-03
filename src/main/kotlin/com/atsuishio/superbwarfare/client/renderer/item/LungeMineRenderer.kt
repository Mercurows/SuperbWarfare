package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.client.animation.AnimationHelper
import com.atsuishio.superbwarfare.client.model.item.LungeMineModel
import com.atsuishio.superbwarfare.item.LungeMine
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.renderer.GeoItemRenderer

open class LungeMineRenderer : GeoItemRenderer<LungeMine>(LungeMineModel()) {
    override fun getRenderType(
        animatable: LungeMine?,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    protected var renderArms: Boolean = false
    protected var currentBuffer: MultiBufferSource? = null
    protected var renderType: RenderType? = null
    var transformType: ItemDisplayContext? = null
    protected var animatable: LungeMine? = null

    override fun renderByItem(
        stack: ItemStack,
        transformType: ItemDisplayContext,
        matrixStack: PoseStack,
        bufferIn: MultiBufferSource,
        combinedLightIn: Int,
        packedOverlay: Int
    ) {
        this.transformType = transformType
        if (this.animatable != null) this.animatable!!.getTransformType(transformType)
        super.renderByItem(stack, transformType, matrixStack, bufferIn, combinedLightIn, packedOverlay)
    }

    override fun actuallyRender(
        matrixStackIn: PoseStack?,
        animatable: LungeMine?,
        model: BakedGeoModel?,
        type: RenderType?,
        renderTypeBuffer: MultiBufferSource?,
        vertexBuilder: VertexConsumer?,
        isRenderer: Boolean,
        partialTicks: Float,
        packedLightIn: Int,
        packedOverlayIn: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        this.currentBuffer = renderTypeBuffer
        this.renderType = type
        this.animatable = animatable
        super.actuallyRender(
            matrixStackIn,
            animatable,
            model,
            type,
            renderTypeBuffer,
            vertexBuilder,
            isRenderer,
            partialTicks,
            packedLightIn,
            packedOverlayIn,
            red,
            green,
            blue,
            alpha
        )
        if (this.renderArms) {
            this.renderArms = false
        }
    }

    override fun renderRecursively(
        stack: PoseStack,
        animatable: LungeMine?,
        bone: GeoBone,
        type: RenderType,
        buffer: MultiBufferSource,
        bufferIn: VertexConsumer,
        isReRender: Boolean,
        partialTick: Float,
        packedLightIn: Int,
        packedOverlayIn: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        val name = bone.name
        var renderingArms = false
        if (name == "Lefthand" || name == "Righthand") {
            bone.setHidden(true)
            renderingArms = true
        } else {
            bone.setHidden(false)
        }

        val player = localPlayer ?: return

        if (this.transformType!!.firstPerson() && renderingArms) {
            AnimationHelper.renderArms(
                player,
                this.renderPerspective,
                stack,
                name,
                bone,
                buffer,
                type,
                packedLightIn,
                false
            )
        }
        super.renderRecursively(
            stack,
            animatable,
            bone,
            type,
            buffer,
            bufferIn,
            isReRender,
            partialTick,
            packedLightIn,
            packedOverlayIn,
            red,
            green,
            blue,
            alpha
        )
    }
}

