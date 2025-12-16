package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object StaminaOverlay : CommonOverlay("stamina") {

    override fun shouldRender() =
        super.shouldRender() && DisplayConfig.STAMINA_HUD.get() && !ClientEventHandler.isEditing

    override fun RenderContext.render() {
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return
        if (ClientEventHandler.switchTime <= 0) return

        guiGraphics.pose().pushPose()

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

        if (ClientEventHandler.exhaustion) {
            RenderSystem.setShaderColor(1f, 0f, 0f, Mth.clamp(ClientEventHandler.switchTime, 0.0, 1.0).toFloat())
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, Mth.clamp(ClientEventHandler.switchTime, 0.0, 1.0).toFloat())
        }

        RenderHelper.fill(
            guiGraphics,
            RenderType.guiOverlay(),
            screenWidth.toFloat() / 2 - 90,
            (screenHeight - 23).toFloat(),
            screenWidth.toFloat() / 2 + 90,
            (screenHeight - 24).toFloat(),
            -90f,
            -16777216
        )
        RenderHelper.fill(
            guiGraphics,
            RenderType.guiOverlay(),
            screenWidth.toFloat() / 2 - 90,
            (screenHeight - 23).toFloat(),
            (screenWidth / 2 + 90 - 1.8 * ClientEventHandler.stamina).toFloat(),
            (screenHeight - 24).toFloat(),
            -90f,
            -1
        )

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        guiGraphics.pose().popPose()
    }
}
