package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.AnimationTimer
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object HeatBarOverlay : CommonOverlay("heat_bar") {
    private val TEXTURE = loc("textures/overlay/heat_bar/heat_bar.png")

    private val ANIMATION_TIMER: AnimationTimer = AnimationTimer(200)
        .animation(AnimationCurves.EASE_IN_QUART)

    override fun shouldRender() = super.shouldRender() && DisplayConfig.ENABLE_HEAT_BAR_HUD.get()

    override fun RenderContext.render() {
        val heat: Double
        val vehicle = player.vehicle

        heat = if (ClientEventHandler.isEditing
            || (player.mainHandItem.item !is GunItem)
            || (vehicle is VehicleEntity && vehicle.banHand(player))
        ) {
            0.0
        } else {
            from(player.mainHandItem).heat.get()
        }

        val currentTime = System.currentTimeMillis()
        if (heat <= 0) {
            ANIMATION_TIMER.forward(currentTime)
        } else {
            ANIMATION_TIMER.beginForward(currentTime)
        }

        if (ANIMATION_TIMER.finished(currentTime)) {
            return
        }

        val poseStack = guiGraphics.pose()
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

        val width = 16
        val height = 64

        val i = (screenWidth - width) / 2
        val j = (screenHeight - height) / 2

        val posX: Float = i + 64 + DisplayConfig.HEAT_BAR_HUD_X_OFFSET.get() + ANIMATION_TIMER.lerp(0f, 5f, currentTime)
        val posY = (j + 6 + DisplayConfig.HEAT_BAR_HUD_Y_OFFSET.get()).toFloat()

        val alpha: Float = ANIMATION_TIMER.lerp(1f, 0f, currentTime)
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha)

        RenderHelper.preciseBlit(
            guiGraphics,
            TEXTURE,
            posX,
            posY,
            0f,
            0f,
            37 / 4f,
            233 / 4f,
            width.toFloat(),
            height.toFloat()
        )

        val rate = Mth.clamp(heat / 100.0, 0.0, 1.0).toFloat()
        val barHeight = 56 * rate

        poseStack.pushPose()

        RenderHelper.blit(
            poseStack,
            TEXTURE,
            posX + 2.5f,
            posY + 1.5f + 56 - barHeight,
            10.5f,
            0f,
            2.25f,
            barHeight,
            width.toFloat(),
            height.toFloat(),
            if (rate >= 0.795f) calculateGradientColor(rate) else 0xFFFFFF
        )

        poseStack.popPose()

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        poseStack.popPose()
    }


    fun calculateGradientColor(rate: Float): Int {
        val clampedRate = Mth.clamp(rate, 0.795f, 1.0f)
        val normalized = (clampedRate - 0.795f) / (1.0f - 0.795f)

        val red = 255
        val green = (255 * (1 - normalized)).toInt()
        val blue = (255 * (1 - normalized)).toInt()

        return (red shl 16) or (green shl 8) or blue
    }
}
