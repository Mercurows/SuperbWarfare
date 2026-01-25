package com.atsuishio.superbwarfare.client.renderer.curio

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.curio.ParachuteModel
import com.atsuishio.superbwarfare.item.curio.ParachuteItem
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.CameraType
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import top.theillusivec4.curios.api.SlotContext
import top.theillusivec4.curios.api.client.ICurioRenderer

class ParachuteRenderer : ICurioRenderer {
    private val model: ParachuteModel = ParachuteModel(mc.entityModels.bakeLayer(ParachuteModel.LAYER_LOCATION))

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

        matrixStack.scale(0.5f, 0.5f, 0.5f)
        matrixStack.translate(0.0, 1.25, 0.0)

        if (stack.getOrCreateTag().getBoolean(ParachuteItem.TAG_OPEN)) {
            val entity = slotContext.entity()
            this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks)
            this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)

            val vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                renderTypeBuffer,
                RenderType.armorCutoutNoCull(TEXTURE),
                false,
                stack.hasFoil()
            )

            model.renderToBuffer(matrixStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f)
        }

        matrixStack.popPose()
    }

    @EventBusSubscriber(bus = EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
    companion object {
        private var firstPersonModel: ParachuteModel? = null
        private val TEXTURE = loc("textures/curio/parachute.png")

        @SubscribeEvent
        fun onRenderLevelStage(event: RenderLevelStageEvent) {
            val buffers = mc.renderBuffers()
            val player = localPlayer ?: return
            if (!ParachuteItem.isParachuteOpen(player)) return
            if (!ParachuteItem.isParachuteVisible(player)) return
            val stack = event.poseStack

            if (event.stage === RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                && mc.options.cameraType == CameraType.FIRST_PERSON
            ) {
                stack.pushPose()

                if (firstPersonModel == null) {
                    firstPersonModel = ParachuteModel(
                        mc.entityModels.bakeLayer(ParachuteModel.LAYER_LOCATION)
                    )
                }

                stack.mulPose(Axis.XP.rotationDegrees(180f))
                stack.mulPose(Axis.YP.rotationDegrees(player.getViewYRot(1f)))
                stack.translate(0.0, 1.5, 0.0)

                firstPersonModel!!.prepareMobModel(player, 0f, 0f, event.partialTick)
                firstPersonModel!!.setupAnim(player, 0f, 0f, player.tickCount.toFloat(), 0f, 0f)
                firstPersonModel!!.renderToBuffer(
                    stack, buffers.bufferSource().getBuffer(
                        RenderType.armorCutoutNoCull(
                            TEXTURE
                        )
                    ), 0xFFFFFF, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f
                )

                stack.popPose()
            }
        }
    }
}
