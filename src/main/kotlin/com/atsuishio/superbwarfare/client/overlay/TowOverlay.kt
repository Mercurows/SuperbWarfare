package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.SodayoPickUpTowEntity
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.min

@OnlyIn(Dist.CLIENT)
object TowOverlay : CommonOverlay("tow") {
    private val SPYGLASS = loc("textures/overlay/spyglass/spyglass.png")

    private var scopeScale = 1f

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()

        val vehicle = player.vehicle;

        if ((vehicle is TowEntity || (vehicle is SodayoPickUpTowEntity && vehicle.turretControllerIndex == vehicle.getSeatIndex(player)))
                && (ClientEventHandler.zoomVehicle || mc.options.cameraType == CameraType.FIRST_PERSON)) {
            poseStack.pushPose()
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableBlend()
            RenderSystem.setShader { GameRenderer.getPositionTexShader() }
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            )
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            scopeScale = Mth.lerp(
                (0.5f * deltaFrame).toDouble(),
                scopeScale.toDouble(),
                1.35f + (0.2f * ClientEventHandler.boltMove)
            ).toFloat()
            val f = min(screenWidth, screenHeight).toFloat()
            val f1: Float = min(screenWidth.toFloat() / f, screenHeight.toFloat() / f) * scopeScale
            val i = Mth.floor(f * f1).toFloat()
            val j = Mth.floor(f * f1).toFloat()
            val k = ((screenWidth - i) / 2)
            val l = ((screenHeight - j) / 2)
            val w = i * 21 / 9
            RenderHelper.preciseBlit(guiGraphics, SPYGLASS, k - (2 * w / 7), l, 0f, 0f, w, j, w, j)
            poseStack.popPose()
        } else {
            scopeScale = 1f
        }
    }
}
