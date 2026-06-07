package com.atsuishio.superbwarfare.client.renderer.armor

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.tools.mc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.model.BedrockArmorModel
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.GeoArmorRenderer
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import java.io.IOException

class HandsomeGogglesRenderer(model: BedrockArmorModel) : GeoArmorRenderer(model, GLASS) {
    companion object {
        val GLASS = Mod.loc("textures/bedrock/vehicle/happiest_ghast_glass.png")
        val TEXTURE = Mod.loc("textures/bedrock/vehicle/happiest_ghast.png")

        private const val CYCLE_TICKS = 80

        /**
         * 预计算的色相偏移帧纹理，索引为帧号 (0..79)。
         * 参考 GeckoLib 的动态贴图方案：预先将所有帧计算好并注册到 TextureManager，
         * 运行时只需切换 ResourceLocation，无需每 tick 做 CPU 像素处理或 GPU 上传。
         */
        private var flowFrames: Array<ResourceLocation?> = arrayOfNulls(CYCLE_TICKS)
        private var flowTexturesReady = false

        @Synchronized
        private fun ensureFlowTexturesLoaded() {
            if (flowTexturesReady) return

            try {
                val resource = mc.resourceManager.getResource(GLASS).get()
                val sourceImage = NativeImage.read(resource.open())

                // 预计算所有 CYCLE_TICKS 帧，每帧对应一个色相偏移量
                for (frame in 0 until CYCLE_TICKS) {
                    val hueShift = frame.toFloat() / CYCLE_TICKS
                    val framePixels = createHueShiftedFrame(sourceImage, hueShift)
                    val frameTexture = DynamicTexture(framePixels)
                    val frameLoc = Mod.loc("textures/bedrock/vehicle/happiest_ghast_glass_flow_$frame")
                    mc.textureManager.register(frameLoc, frameTexture)
                    flowFrames[frame] = frameLoc
                }

                sourceImage.close()
                flowTexturesReady = true
            } catch (_: IOException) {
                // 加载失败时 flowTexturesReady 保持 false，渲染回退到原始贴图
            }
        }

        /**
         * 根据 tickCount 获取当前帧对应的预计算纹理 ResourceLocation。
         * 无需任何 CPU 像素处理或 GPU 上传——直接返回已注册的纹理。
         */
        private fun getFlowFrame(tickCount: Int): ResourceLocation {
            val frame = tickCount % CYCLE_TICKS
            return flowFrames[frame] ?: GLASS
        }

        /**
         * 创建一个色相偏移后的 NativeImage。
         * 对每个像素做 HSV 色相偏移，保留原始饱和度和明度。
         * 此方法仅在初始化时调用 CYCLE_TICKS 次，运行时不再调用。
         */
        private fun createHueShiftedFrame(source: NativeImage, hueShift: Float): NativeImage {
            val dest = NativeImage(source.width, source.height, true)

            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    val argb = source.getPixelRGBA(x, y)
                    val a = (argb shr 24) and 0xFF
                    if (a == 0) {
                        dest.setPixelRGBA(x, y, 0)
                        continue
                    }

                    val r = (argb shr 16) and 0xFF
                    val g = (argb shr 8) and 0xFF
                    val b = argb and 0xFF

                    val (h, s, v) = rgbToHsv(r / 255f, g / 255f, b / 255f)
                    val newHue = (h + hueShift) % 1f
                    val (nr, ng, nb) = hsvToRgbScalar(newHue, s, v)

                    val ir = (nr * 255).toInt().coerceIn(0, 255)
                    val ig = (ng * 255).toInt().coerceIn(0, 255)
                    val ib = (nb * 255).toInt().coerceIn(0, 255)

                    dest.setPixelRGBA(x, y, (a shl 24) or (ir shl 16) or (ig shl 8) or ib)
                }
            }
            return dest
        }

        // ========== RGB ↔ HSV 转换 ==========

        /** RGB 0-1 → HSV (Hue 0-1, Sat 0-1, Val 0-1) */
        private fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            val h = when {
                delta == 0f -> 0f
                max == r -> ((g - b) / delta) % 6f
                max == g -> ((b - r) / delta) + 2f
                else -> ((r - g) / delta) + 4f
            }.let { raw ->
                if (raw < 0f) raw + 6f else raw
            } / 6f

            val s = if (max == 0f) 0f else delta / max

            return Triple(h, s, max)
        }

        /** HSV → RGB 0-1：保留纹理原本的饱和度和明度，只偏移色相 */
        private fun hsvToRgbScalar(h: Float, s: Float, v: Float): Triple<Float, Float, Float> {
            if (s == 0f) return Triple(v, v, v)

            val hue6 = h * 6f
            val sector = hue6.toInt()
            val fraction = hue6 - sector

            val p = v * (1f - s)
            val q = v * (1f - s * fraction)
            val t = v * (1f - s * (1f - fraction))

            return when (sector % 6) {
                0 -> Triple(v, t, p)
                1 -> Triple(q, v, p)
                2 -> Triple(p, v, t)
                3 -> Triple(p, q, v)
                4 -> Triple(t, p, v)
                5 -> Triple(v, p, q)
                else -> Triple(v, v, v)
            }
        }
    }

    override fun renderToBuffer(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        light: Int,
        overlay: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val mc = mc
        val bufferSource = mc.renderBuffers().bufferSource()
        val partialTick = mc.frameTime

        poseStack.pushPose()
        if (this.livingEntity != null && this.equipmentSlot != null && this.original != null) {
            scaleModelForBaby(poseStack, this.livingEntity, partialTick, this.equipmentSlot, this.original)
        }

        model.renderToBuffer(
            poseStack,
            bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE)),
            light,
            overlay,
            r,
            g,
            b,
            a
        )
        model.renderToBuffer(
            poseStack,
            bufferSource.getBuffer(RenderType.eyes(this.texture)),
            light,
            overlay,
            r,
            g,
            b,
            a
        )
        poseStack.popPose()

        afterRender(poseStack, buffer, light, overlay, r, g, b, a)
    }

    override fun getTexture(): ResourceLocation {
        ensureFlowTexturesLoaded()
        val tickCount = mc.level?.gameTime?.toInt() ?: 0
        return if (flowTexturesReady) {
            getFlowFrame(tickCount)
        } else {
            GLASS
        }
    }
}