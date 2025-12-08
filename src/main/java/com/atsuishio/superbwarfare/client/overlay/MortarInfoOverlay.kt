package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.overlay.components.CENTER
import com.atsuishio.superbwarfare.client.overlay.components.LEFT_BOTTOM
import com.atsuishio.superbwarfare.client.overlay.components.RIGHT_BOTTOM
import com.atsuishio.superbwarfare.client.overlay.components.StringComponent
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object MortarInfoOverlay : CommonOverlay("mortar_info") {

    val BASE_POINT = CENTER.offset(-90F, -26F)

    val PITCH = StringComponent(BASE_POINT)
    val YAW = StringComponent(BASE_POINT.addOffsetY(10F))
    val RANGE = StringComponent(BASE_POINT.addOffsetY(20F))

    // TODO 移除测试组件
    val AAA = StringComponent(CENTER.offset(10F, -10F), LEFT_BOTTOM).apply {
        component = Component.literal("AAAAAAAA")
        color = 0xFF0000
    }
    val BBB = StringComponent(CENTER.offset(20F, 20F)).apply {
        component = Component.literal("BBBBBBBB")
        color = 0x00FF00
    }
    val CCC = StringComponent(RIGHT_BOTTOM.offset(-30F, -30F), RIGHT_BOTTOM).apply {
        component = Component.literal("CCCCCCCC")
        color = 0x0000FF
    }

    init {
        registerComponents(PITCH, YAW, RANGE, AAA, BBB, CCC)
    }

    var mortar: MortarEntity? = null

    override fun shouldRender(): Boolean {
        if (!super.shouldRender()) return false

        mortar = TraceTool.findLookingEntity(localPlayer, 6.0) as? MortarEntity ?: return false

        return true
    }

    override fun RenderContext.preRender() {
        val mortar = mortar ?: return

        PITCH.component =
            Component.translatable("tips.superbwarfare.mortar.pitch") + format1D(-mortar.xRot.toDouble(), "°")

        YAW.component = Component.translatable("tips.superbwarfare.mortar.yaw") + format1D(mortar.yRot.toDouble(), "°")

        RANGE.component = Component.translatable("tips.superbwarfare.mortar.range") + format1D(
            getRange(
                -mortar.xRot.toDouble(),
                mortar.getProjectileVelocity("Main").toDouble(),
                mortar.getProjectileGravity("Main").toDouble()
            ).toInt().toDouble(), "m"
        )
    }
}
