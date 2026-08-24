package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.entity.vehicle.AirSheepEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.DyeColor

class AirSheepRenderer(manager: EntityRendererProvider.Context) :
    GeoVehicleRenderer<AirSheepEntity>(manager) {

    override fun renderCustomPart(
        entity: AirSheepEntity,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(entity, instance, poseStack, entityYaw, partialTicks, buffer, packedLight)
        val emissive = this.getEmissiveTextureLocation(poseStack, entity) ?: return

        val renderType = RenderType.entityTranslucent(emissive)
        var packedLight = packedLight

        val id: Int = entity.colorId

        var color: FloatArray?

        if (entity.customName != null && entity.customName!!.string == "jeb_") {
            color = getRainbowColorHSL(entity.tickCount)
            packedLight = LightTexture.FULL_BRIGHT
        } else {
            val intColor = DyeColor.byId(id).textureDiffuseColor
            color = floatArrayOf(
                ((intColor shr 16) and 0xFF).toFloat(),
                ((intColor shr 8) and 0xFF).toFloat(),
                (intColor and 0xFF).toFloat()
            )
        }

        if (ClientEventHandler.activeThermalImaging) {
            color = floatArrayOf(1f, 1f, 1f)
            packedLight = LightTexture.FULL_BRIGHT
        }

        instance.renderToBuffer(
            poseStack,
            buffer,
            renderType,
            BedrockModelRenderTypes.polyMeshCutout(emissive),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            color[0] / 255.0f,
            color[1] / 255.0f,
            color[2] / 255.0f,
            1f
        )
    }

    /**
     * 通过调整色相(H)来实现RGB灯效，使用HSV模型：
     * - H(色相)随时间循环，产生彩虹渐变
     * - S(饱和度)固定为1.0，颜色始终饱和
     * - V(明度)固定为1.0，始终保持最大亮度（RGB灯效特征：至少一个通道为最大值）
     */
    fun getRainbowColorHSL(tickCount: Int): FloatArray {
        // 完整循环的tick数，调整这个值控制变化速度
        val cycleTicks = 80

        // 计算色相（0-1范围）
        val hue = (tickCount % cycleTicks) / cycleTicks.toFloat()

        // 固定饱和度和明度（HSV模型，V=1保证RGB灯效的鲜艳明亮）
        val saturation = 1.0f
        val value = 1.0f

        return hsvToRgb(hue, saturation, value)
    }

    /**
     * HSV→RGB转换：仅通过调整色相(H)来改变颜色，
     * S和V固定时，输出RGB至少有一个通道为1.0，符合RGB灯效特征
     */
    fun hsvToRgb(h: Float, s: Float, v: Float): FloatArray {
        if (s == 0f) {
            return floatArrayOf(v, v, v, 1.0f)
        }

        val hue = h * 6f
        val sector = hue.toInt()       // 色相扇区 0-5
        val fraction = hue - sector     // 扇区内插值 0-1
        val p = v * (1f - s)
        val q = v * (1f - s * fraction)
        val t = v * (1f - s * (1f - fraction))

        val (r, g, b) = when (sector % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            5 -> Triple(v, p, q)
            else -> Triple(v, v, v)  // unreachable
        }

        return floatArrayOf(r, g, b, 1.0f)
    }
}
