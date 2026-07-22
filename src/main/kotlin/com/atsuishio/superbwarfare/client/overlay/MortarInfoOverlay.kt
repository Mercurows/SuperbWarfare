package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.overlay.components.CENTER
import com.atsuishio.superbwarfare.client.overlay.components.StringComponent
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object MortarInfoOverlay : CommonOverlay("mortar_info") {
    val BASE_POINT = CENTER.offset(-90F, -26F)
    val PITCH = StringComponent(BASE_POINT)
    val YAW = StringComponent(BASE_POINT.offsetY(10F))
    val RANGE = StringComponent(BASE_POINT.offsetY(20F))

    init {
        registerComponents(PITCH, YAW, RANGE)
    }

    override fun RenderContext.preRender() {
        val mortar = OverlayTraceHandler.playerReachEntity as? MortarEntity ?: return

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
