package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.HappiestGhastEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import java.io.IOException

class HappiestGhastRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : HappiestGhastEntity, T : BasicGeoVehicleEntity {

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val turretRight = model.getBone("turret_right")
        if (turretRight != null) {
            turretRight.rotation.rotationY(turretYRot * Mth.DEG_TO_RAD)
            turretRight.visible = !(vehicle.isWreck && vehicle.hasTurret() && vehicle.sympatheticDetonated)
        }
    }

    override fun renderCustomPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)

        // 确保动态纹理已加载，然后逐像素偏移色相
        ensureFlowTextureLoaded()
        updateFlowTexture(vehicle.tickCount)

        // 使用动态纹理的 ResourceLocation，替代原始玻璃贴图
        val texLocation = if (flowTextureReady && !vehicle.sympatheticDetonated) FLOW_GLASS else GLASS

        val renderType = RenderType.entityTranslucent(texLocation)
        val renderTypeLight = RenderType.eyes(texLocation)
        val polyMeshType = BedrockModelRenderTypes.polyMeshCutout(texLocation)

        model.renderToBuffer(
            poseStack, buffer, renderType, polyMeshType,
            packedLight, OverlayTexture.NO_OVERLAY,
            1f, 1f, 1f, 1f
        )

        if (!vehicle.sympatheticDetonated) {
            model.renderToBuffer(
                poseStack, buffer, renderTypeLight, polyMeshType,
                packedLight, OverlayTexture.NO_OVERLAY,
                1f, 1f, 1f, 1f
            )
        }
    }

    companion object {
        val GLASS = Mod.loc("textures/bedrock/vehicle/happiest_ghast_glass.png")
        val FLOW_GLASS = Mod.loc("textures/bedrock/vehicle/happiest_ghast_glass_flow")
        private const val CYCLE_TICKS = 80

        /** 原始贴图像素（只读，不会被修改） */
        private var sourceImage: NativeImage? = null
        /** 输出缓冲区（写入色相偏移后的像素） */
        private var destImage: NativeImage? = null
        /** 动态纹理实例 */
        private var flowTexture: DynamicTexture? = null
        /** 上一次更新的色相偏移值，用于避免重复上传 */
        private var lastHueShift = Float.NaN
        /** 动态纹理是否可用 */
        private var flowTextureReady = false

        @Synchronized
        private fun ensureFlowTextureLoaded() {
            if (flowTextureReady || sourceImage != null) return

            try {
                val mc = Minecraft.getInstance()
                val resource = mc.resourceManager.getResource(GLASS).get()
                sourceImage = NativeImage.read(resource.open())

                val src = sourceImage!!
                // 创建同尺寸的输出缓冲区
                destImage = NativeImage(src.width, src.height, true)
                // 用输出缓冲区创建 DynamicTexture
                flowTexture = DynamicTexture(destImage!!)
                mc.textureManager.register(FLOW_GLASS, flowTexture)
                flowTextureReady = true
            } catch (_: IOException) {
                // 加载失败时 flowTextureReady 保持 false，渲染回退到原始贴图
            }
        }

        /**
         * 对贴图的每个像素做 HSV 色相偏移，实现彩虹渐变的无限循环流动效果。
         *
         * 工作流程：
         * 1. 读取原始贴图像素 → ARGB 格式
         * 2. 提取 R/G/B → 转为 HSV
         * 3. H += 当前帧偏移量（0→1 循环）
         * 4. HSV → RGB → 写回输出缓冲区
         * 5. 上传到 GPU
         *
         * 效果：贴图上"红橙黄绿青蓝紫"的渐变会整体向前滚动，
         * 即 "红橙黄绿青蓝紫" → "橙黄绿青蓝紫红" → "黄绿青蓝紫红橙" → ... 无限循环。
         */
        @Synchronized
        fun updateFlowTexture(tickCount: Int) {
            val source = sourceImage ?: return
            val dest = destImage ?: return
            val texture = flowTexture ?: return

            // 色相偏移量：0→1 循环，每 CYCLE_TICKS 完成一个完整周期
            val hueShift = ((tickCount % CYCLE_TICKS).toFloat() / CYCLE_TICKS)
            if (hueShift == lastHueShift) return
            lastHueShift = hueShift

            for (y in 0 until source.height) {
                for (x in 0 until source.width) {
                    // NativeImage ARGB 格式：bits 24-31=A, 16-23=R, 8-15=G, 0-7=B
                    val argb = source.getPixelRGBA(x, y)
                    val a = (argb shr 24) and 0xFF
                    if (a == 0) {
                        dest.setPixelRGBA(x, y, 0)
                        continue
                    }

                    val r = (argb shr 16) and 0xFF
                    val g = (argb shr 8) and 0xFF
                    val b = argb and 0xFF

                    // RGB 0-255 → 0-1 → HSV，偏移色相，HSV → RGB 0-1 → 0-255
                    val (h, s, v) = rgbToHsv(r / 255f, g / 255f, b / 255f)
                    val newHue = (h + hueShift) % 1f
                    val (nr, ng, nb) = hsvToRgbScalar(newHue, s, v)

                    val ir = (nr * 255).toInt().coerceIn(0, 255)
                    val ig = (ng * 255).toInt().coerceIn(0, 255)
                    val ib = (nb * 255).toInt().coerceIn(0, 255)

                    val newArgb = (a shl 24) or (ir shl 16) or (ig shl 8) or ib
                    dest.setPixelRGBA(x, y, newArgb)
                }
            }
            texture.upload()
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
            val v = max

            return Triple(h, s, v)
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
}
