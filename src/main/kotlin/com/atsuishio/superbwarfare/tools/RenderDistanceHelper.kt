package com.atsuishio.superbwarfare.tools

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

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
        // TODO 换成配置项
        val globalLODDistance = 32
        if (distance < globalLODDistance) return false
        val matrix = poseStack.last().pose()
        val viewDistance = matrix.m30() * matrix.m30() + matrix.m31() * matrix.m31() + matrix.m32() * matrix.m32()
        return viewDistance >= distance * distance
    }
}