package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.TinySpeedboatEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.item.DyeColor

class TinySpeedboatRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : TinySpeedboatEntity, T : BasicGeoVehicleEntity {

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)
        val control = model.getBone("control")
        val rudder = model.getBone("rudder")

        control.rotation.rotationZ(-3 * Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
        rudder.rotation.rotationY(Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
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

        var renderType = RenderType.entityTranslucent(getColorTextureLocation())

        val id: Int = vehicle.colorId

        var color: FloatArray?

        if (vehicle.customName != null && vehicle.customName!!.string == "jeb_") {
            color = getRainbowColorHSL(vehicle.tickCount)
            renderType = RenderType.entityTranslucentEmissive(getColorTextureLocation())
        } else {
            val afloat = DyeColor.byId(id).textureDiffuseColors
            color = floatArrayOf(afloat[0], afloat[1], afloat[2])
        }

        if (ClientEventHandler.activeThermalImaging) {
            color = floatArrayOf(1f, 1f, 1f, 1.0f)
            renderType = RenderType.entityTranslucentEmissive(getColorTextureLocation())
        }

        model.renderToBuffer(
            poseStack,
            buffer,
            renderType,
            BedrockModelRenderTypes.polyMeshCutout(getColorTextureLocation()),
            packedLight,
            OverlayTexture.NO_OVERLAY,
            color[0],
            color[1],
            color[2],
            1f
        )
    }

    fun getColorTextureLocation(): ResourceLocation {
        return Mod.loc("textures/bedrock/vehicle/tiny_speedboat_color.png")
    }

    fun getRainbowColorHSL(tickCount: Int): FloatArray {
        // 完整循环的tick数，调整这个值控制变化速度
        val cycleTicks = 80

        // 计算色相（0-1范围）
        val hue = (tickCount % cycleTicks) / cycleTicks.toFloat()

        // 固定饱和度和亮度
        val saturation = 1.0f
        val lightness = 0.5f

        return hslToRgb(hue, saturation, lightness)
    }

    // HSL转RGB转换函数
    fun hslToRgb(h: Float, s: Float, l: Float): FloatArray {
        val r: Float
        val g: Float
        val b: Float

        if (s == 0f) {
            b = l
            g = b
            r = g
        } else {
            val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            r = hueToRgb(p, q, h + 1f / 3f)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1f / 3f)
        }

        return floatArrayOf(r, g, b, 1.0f) // Alpha保持1.0
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var t = t
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        if (t < 1f / 6f) return p + (q - p) * 6f * t
        if (t < 1f / 2f) return q
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
        return p
    }
}
