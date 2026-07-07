package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.mojang.blaze3d.vertex.PoseStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object RenderDistanceHelper {
    private var GUI_RENDER_TIMESTAMP: Long = -1

    @JvmStatic
    fun markGuiRenderTimestamp() {
        GUI_RENDER_TIMESTAMP = System.currentTimeMillis()
    }

    @JvmStatic
    fun isInGui(): Boolean {
        return System.currentTimeMillis() - GUI_RENDER_TIMESTAMP < 100L
    }

    @JvmStatic
    fun shouldRenderLOD(poseStack: PoseStack, distance: Double): Boolean {
        if (isInGui()) return false
        if (distance <= 0) return false
        // PJM: TODO закрыт — базовая дистанция LOD берётся из конфига; авторские
        // дистанции моделей (32/64/96) масштабируются относительно стандартных 32
        val scaled = distance * DisplayConfig.VEHICLE_LOD_DISTANCE.get() / 32.0
        val matrix = poseStack.last().pose()
        val viewDistance = matrix.m30() * matrix.m30() + matrix.m31() * matrix.m31() + matrix.m32() * matrix.m32()
        return viewDistance >= scaled * scaled
    }
}