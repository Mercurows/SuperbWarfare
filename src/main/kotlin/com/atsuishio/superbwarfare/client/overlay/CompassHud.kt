package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
 * 载具指南针HUD组件
 * 表盘（base）随载具yRot转动，指针（needle）固定不动指向正北
 * 可在任意HUD中引用，通过 [x]、[y]、[size] 自由调节位置和大小
 */
@OnlyIn(Dist.CLIENT)
class CompassHud {
    companion object {
        val DEFAULT_BASE: ResourceLocation = loc("textures/overlay/vehicle/common/compass_base.png")
        val DEFAULT_NEEDLE: ResourceLocation = loc("textures/overlay/vehicle/common/compass_needle.png")
    }

    /** 表盘贴图 */
    var baseTexture: ResourceLocation = DEFAULT_BASE
    /** 指针贴图 */
    var needleTexture: ResourceLocation = DEFAULT_NEEDLE

    /**
     * X坐标（屏幕坐标）。
     * 正值相对于屏幕左侧，负值相对于屏幕右侧（screenWidth + x）。
     */
    var x: Float = 0f

    /**
     * Y坐标（屏幕坐标）。
     * 正值相对于屏幕顶部，负值相对于屏幕底部（screenHeight + y）。
     */
    var y: Float = 0f

    /** 指南针渲染尺寸（正方形，同时控制宽高） */
    var size: Float = 64f

    /**
     * 渲染指南针
     *
     * @param guiGraphics 渲染上下文
     * @param vehicle     载具实体，用于获取yRot
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     */
    fun render(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        screenWidth: Int,
        screenHeight: Int,
        partialTick: Float
    ) {
        val actualX = if (x < 0) screenWidth + x else x
        val actualY = if (y < 0) screenHeight + y else y

        val centerX = actualX + size / 2
        val centerY = actualY + size / 2

        val poseStack = guiGraphics.pose()

        // 绘制旋转表盘——表盘跟随载具方向转动
        poseStack.pushPose()
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-vehicle.getYaw(partialTick) - 180), centerX, centerY, 0f)
        RenderHelper.preciseBlit(
            guiGraphics,
            baseTexture,
            actualX, actualY,
            0f, 0f,
            size, size,
            size, size
        )
        poseStack.popPose()

        // 绘制固定指针——指针始终指向上方（正北）
        RenderHelper.preciseBlit(
            guiGraphics,
            needleTexture,
            actualX, actualY,
            0f, 0f,
            size, size,
            size, size
        )
    }
}
