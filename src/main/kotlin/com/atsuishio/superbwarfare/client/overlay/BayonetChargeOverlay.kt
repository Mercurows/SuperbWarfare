package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.AnimationTimer
import com.atsuishio.superbwarfare.client.gun.GunChargeHandler
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object BayonetChargeOverlay : CommonOverlay("bayonet_charge") {
    private val TEXTURE = loc("textures/overlay/crosshair/bayonet_charge.png")

    // 贴图原尺寸 128 x 16，绘制为 48 x 6；填充同样在贴图像素上按比例裁
    private const val TEX_WIDTH = 128f
    private const val TEX_HEIGHT = 16f
    private const val SHRINK = 0.375f
    private const val WIDTH = TEX_WIDTH * SHRINK
    private const val HEIGHT = TEX_HEIGHT * SHRINK

    // 屏幕居中往下：条子顶端在屏幕中心以下这么多像素
    private const val OFFSET_Y = 30f

    // 冷却满了以后全白停留的时长，之后才开始渐隐
    private const val HOLD_MS = 1000L

    private val ANIMATION_TIMER: AnimationTimer = AnimationTimer(200)
        .animation(AnimationCurves.EASE_IN_QUART)

    private enum class Phase { IDLE, CHARGE, COOLDOWN, FULL }

    private var phase = Phase.IDLE
    private var holdUntil = 0L

    override fun RenderContext.render() {
        val now = System.currentTimeMillis()
        val charging = GunChargeHandler.isCharging()
        val cooling = GunChargeHandler.cooldownTicks > 0

        // 冲锋/冷却期间每帧把计时器按在起点（透明度即 1），只有停留结束才放它往前走，走完 = 淡出完毕
        when {
            charging -> {
                phase = Phase.CHARGE
                ANIMATION_TIMER.beginForward(now)
            }

            cooling -> {
                phase = Phase.COOLDOWN
                ANIMATION_TIMER.beginForward(now)
            }

            else -> {
                // 窗口或冷却刚走完：整条变全白
                if (phase == Phase.CHARGE || phase == Phase.COOLDOWN) {
                    phase = Phase.FULL
                    holdUntil = now + HOLD_MS
                    ANIMATION_TIMER.beginForward(now)
                }
                if (phase == Phase.FULL) {
                    if (now < holdUntil) ANIMATION_TIMER.beginForward(now) else ANIMATION_TIMER.forward(now)
                    if (ANIMATION_TIMER.finished(now)) phase = Phase.IDLE
                }
            }
        }

        if (phase == Phase.IDLE) return

        val alpha = ANIMATION_TIMER.lerp(1f, 0f, now)

        // 都从左端（刀尖）起算：冲锋时白条随进度缩回左边，冷却时红条往右长
        val rate = when (phase) {
            Phase.CHARGE ->
                GunChargeHandler.chargeTicks.toFloat() / GunChargeHandler.chargeTotal.coerceAtLeast(1)

            Phase.COOLDOWN ->
                1f - GunChargeHandler.cooldownTicks.toFloat() / GunChargeHandler.cooldownTotal.coerceAtLeast(1)

            else -> 1f
        }.coerceIn(0f, 1f)

        val posX = (w - WIDTH) / 2f
        val posY = h / 2f + OFFSET_Y

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )

        // 底：深灰半透明
        RenderSystem.setShaderColor(0.2f, 0.2f, 0.22f, 0.7f * alpha)
        RenderHelper.blit(poseStack, TEXTURE, posX, posY, WIDTH, HEIGHT)

        // 填充：从左端（刀尖）裁同一张贴图
        if (rate > 0f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
            RenderHelper.blitRegion(
                poseStack, TEXTURE, posX, posY, WIDTH * rate, HEIGHT,
                u = 0f, v = 0f, uWidth = TEX_WIDTH * rate, vHeight = TEX_HEIGHT,
                textureWidth = TEX_WIDTH, textureHeight = TEX_HEIGHT,
                argb = RenderHelper.opaque(if (phase == Phase.COOLDOWN) 0xFF0000 else 0xFFFFFF),
            )
        }

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        poseStack.popPose()
    }
}
