package com.atsuishio.superbwarfare.client.renderer.curio

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.curio.ThermalImagingGogglesModel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.client.ICurioRenderer

class ThermalImagingGogglesRenderer : ICurioRenderer {
    private val model: ThermalImagingGogglesModel = ThermalImagingGogglesModel(
        mc.entityModels.bakeLayer(ThermalImagingGogglesModel.LAYER_LOCATION)
    )

    override fun <T : LivingEntity?, M : EntityModel<T?>?> render(
        stack: ItemStack,
        slotContext: SlotContext,
        matrixStack: PoseStack,
        renderLayerParent: RenderLayerParent<T?, M?>?,
        renderTypeBuffer: MultiBufferSource,
        light: Int,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        matrixStack.pushPose()
        val entity = slotContext.entity()
        this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks)
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
        ICurioRenderer.followHeadRotations(entity, this.model.bone)
        val vertexConsumer = ItemRenderer.getArmorFoilBuffer(
            renderTypeBuffer,
            RenderType.armorCutoutNoCull(TEXTURE),
            false,
            stack.hasFoil()
        )
        model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f)
        matrixStack.popPose()
    }

    companion object {
        private val TEXTURE = loc("textures/curio/thermal_imaging_goggles.png")
    }
}
