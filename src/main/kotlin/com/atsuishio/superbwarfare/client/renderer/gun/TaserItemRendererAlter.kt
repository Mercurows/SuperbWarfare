package com.atsuishio.superbwarfare.client.renderer.gun

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.animation.item.TaserItemAnimationInstance
import com.atsuishio.superbwarfare.resource.ModResourceManager
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.animation.IFPAnimationInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.HandedBedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.AbstractGeoItemRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.apache.commons.lang3.tuple.Pair
import org.joml.Matrix4f

class TaserItemRendererAlter : AbstractGeoItemRenderer<HandedBedrockModel>() {
    override fun getModelAndRenderType(stack: ItemStack): Pair<HandedBedrockModel, RenderType> {
        return Pair.of(
            ModResourceManager.modResources.taserItemModel,
            RenderType.entityTranslucent(loc("textures/item/taser.png"))
        )
    }

    override fun getLodModelAndRenderType(stack: ItemStack): Pair<HandedBedrockModel, RenderType> {
        return this.getModelAndRenderType(stack)
    }

    override fun getSlotTexture(stack: ItemStack): ResourceLocation {
        return loc("textures/item/taser_icon.png")
    }

    override fun renderFirstPerson(
        player: LocalPlayer,
        stack: ItemStack,
        ctx: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        light: Int,
        partialTick: Float
    ) {
        if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            super.renderFirstPerson(player, stack, ctx, poseStack, bufferSource, light, partialTick)
        }
    }

    override fun createAnimationInstance(
        stack: ItemStack,
        entity: Entity
    ): IFPAnimationInstance? {
        if (entity is LocalPlayer) {
            return TaserItemAnimationInstance(
                entity,
                stack,
                ModResourceManager.modResources.taserItemAnimation,
                ModResourceManager.modResources.taserItemModel
            )
        }
        return super.createAnimationInstance(stack, entity)
    }

    override fun beforeRender(
        poseStack: PoseStack,
        ctx: ItemDisplayContext,
        model: HandedBedrockModel,
        stack: ItemStack,
        partialTicks: Float
    ) {
        model.isRenderHand = ctx.firstPerson()
        if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            val ani = FirstPersonRenderHandler.getActiveAnimationInstance()
            if (ani != null) {
                model.applyPose(ani.cachedPose)
                val idleViewMatrix = Matrix4f(model.getBone("camera").globalTransform)
                poseStack.mulPose(idleViewMatrix.invert())
            }
        }
    }

    override fun afterRender(
        poseStack: PoseStack,
        ctx: ItemDisplayContext,
        model: HandedBedrockModel,
        stack: ItemStack,
        bufferSource: MultiBufferSource,
        light: Int,
        partialTicks: Float
    ) {
        model.applyPose(model.bindPose)
    }

    override fun blockOffhandRender(): Boolean = true

    override fun render(
        stack: ItemStack,
        ctx: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        light: Int,
        overlay: Int,
        partialTicks: Float
    ) {
        super.render(stack, ctx, poseStack, bufferSource, light, overlay, partialTicks)
    }

    override fun renderByItem(
        stack: ItemStack,
        ctx: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return
        super.renderByItem(stack, ctx, poseStack, bufferSource, light, overlay)
    }
}