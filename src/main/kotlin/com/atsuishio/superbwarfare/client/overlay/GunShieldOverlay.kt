package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.data.gun.ActiveGun
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ShieldRuntime
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.util.Mth
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object GunShieldOverlay : CommonOverlay("gun_shield") {
    private const val OUTER_RADIUS = 80f
    private const val INNER_RADIUS = 72f
    private const val BORDER_WIDTH = 1.5f
    private const val START_ANGLE = 210.0
    private const val SWEEP_ANGLE = 120.0
    private const val END_INSET_ANGLE = 1.2
    private const val ARC_SEGMENTS = 64

    private const val HIP_SCALE = 0.275f
    private const val HIP_OPACITY = 0.8f
    private const val HIP_OFFSET = 40f
    private const val ADS_REFERENCE_HEIGHT = 480f
    private const val ADS_TOP_RATIO = 0.09f
    private const val FONT_SCALE = 1.8f

    override fun shouldRender() = super.shouldRender()
            && !MiscConfig.HIDE_COMBAT_HUD.get()
            && !ClientEventHandler.isEditing

    override fun RenderContext.render() {
        // Shields protect the main-hand gun, including while its sub-weapon is deployed.
        val gun = ActiveGun.mainGun(player) ?: return
        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.banHand(player)) return

        val shields = ShieldRuntime.of(gun)
        if (shields.isEmpty()) return

        val capacity = shields.sumOf { it.maxCharge().coerceAtLeast(0.0) }
        val charge = shields.sumOf { it.charge.coerceIn(0.0, it.maxCharge().coerceAtLeast(0.0)) }
        val progress = if (capacity > 0.0) (charge / capacity).coerceIn(0.0, 1.0) else 0.0
        // Round up so a shield with a positive fractional charge never reads as broken.
        val label = ceil(charge).toLong().toString()

        val aim = if (isFirstPerson) ClientEventHandler.aimingProgress(ClientEventHandler.zoomTime) else 0f
        val scale = Mth.lerp(aim, HIP_SCALE, screenHeight / ADS_REFERENCE_HEIGHT)
        val opacity = Mth.lerp(aim, HIP_OPACITY, 1f)
        val apexY = Mth.lerp(aim, screenHeight / 2f - HIP_OFFSET, screenHeight * ADS_TOP_RATIO)
        val pose = guiGraphics.pose()
        guiGraphics.flush()
        pose.pushPose()
        try {
            pose.translate(screenWidth / 2.0, apexY.toDouble(), 0.0)
            pose.scale(scale, scale, 1f)

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShader { GameRenderer.getPositionColorShader() }
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            // Draw adjoining regions so translucent fill does not blend over the rim/background.
            val rimColor = 0xFFD0D0D0.toInt()
            val fillOuter = OUTER_RADIUS - BORDER_WIDTH
            val fillInner = INNER_RADIUS + BORDER_WIDTH
            drawArc(OUTER_RADIUS, fillOuter, START_ANGLE, SWEEP_ANGLE, rimColor, opacity)
            drawArc(fillInner, INNER_RADIUS, START_ANGLE, SWEEP_ANGLE, rimColor, opacity)
            drawArc(fillOuter, fillInner, START_ANGLE, END_INSET_ANGLE, rimColor, opacity)
            drawArc(fillOuter, fillInner, START_ANGLE + SWEEP_ANGLE - END_INSET_ANGLE,
                END_INSET_ANGLE, rimColor, opacity)
            val fillStart = START_ANGLE + END_INSET_ANGLE
            val fillSweep = SWEEP_ANGLE - 2 * END_INSET_ANGLE
            val chargedSweep = fillSweep * progress
            if (progress > 0.0) {
                drawArc(fillOuter, fillInner, fillStart, chargedSweep, 0xFFFFFFFF.toInt(), opacity)
            }
            if (progress < 1.0) {
                drawArc(fillOuter, fillInner, fillStart + chargedSweep, fillSweep - chargedSweep,
                    0xFF4A4A4A.toInt(), opacity)
            }

            RenderSystem.disableBlend()
            pose.scale(FONT_SCALE, FONT_SCALE, 1f)
            guiGraphics.drawString(mc.font, label, -mc.font.width(label) / 2f,
                -mc.font.lineHeight - 4f, ((255 * opacity).toInt() shl 24) or 0xFFFFFF, false)
            guiGraphics.flush()
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
            pose.popPose()
        }
    }

    private fun RenderContext.drawArc(
        outer: Float, inner: Float, start: Double, sweep: Double, color: Int, opacity: Float
    ) {
        val matrix = guiGraphics.pose().last().pose()
        val tesselator = Tesselator.getInstance()
        val buffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)
        val red = color shr 16 and 255
        val green = color shr 8 and 255
        val blue = color and 255
        val alpha = ((color ushr 24) * opacity).toInt()
        for (segment in 0..ARC_SEGMENTS) {
            val angle = Math.toRadians(start + sweep * segment / ARC_SEGMENTS)
            val x = cos(angle).toFloat()
            val y = sin(angle).toFloat()
            buffer.addVertex(matrix, outer * x, OUTER_RADIUS + outer * y, 0f)
                .setColor(red, green, blue, alpha)
            buffer.addVertex(matrix, inner * x, OUTER_RADIUS + inner * y, 0f)
                .setColor(red, green, blue, alpha)
        }
        // 1.21 的 Tesselator.begin 只返回未上传的 BufferBuilder；必须 build 后交给
        // BufferUploader 才会真正画出来（1.20 的 tesselator.end() 在 1.21 已移除）。
        BufferUploader.drawWithShader(buffer.buildOrThrow())
    }
}
