package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.font
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object MortarInfoOverlay : CommonOverlay("mortar_info") {

    override fun RenderContext.renderOverlay() {
        val lookingEntity = TraceTool.findLookingEntity(player, 6.0)

        if (lookingEntity is MortarEntity) {
            guiGraphics.drawString(
                font, Component.translatable("tips.superbwarfare.mortar.pitch")
                        + format1D(-lookingEntity.xRot.toDouble(), "°"),
                w / 2 - 90, h / 2 - 26, -1, false
            )
            guiGraphics.drawString(
                font, Component.translatable("tips.superbwarfare.mortar.yaw")
                        + format1D(lookingEntity.yRot.toDouble(), "°"),
                w / 2 - 90, h / 2 - 16, -1, false
            )
            guiGraphics.drawString(
                font, Component.translatable("tips.superbwarfare.mortar.range") + format1D(
                    getRange(
                        -lookingEntity.xRot.toDouble(),
                        lookingEntity.getProjectileVelocity("Main").toDouble(),
                        lookingEntity.getProjectileGravity("Main").toDouble()
                    ).toInt().toDouble(), "m"
                ),
                w / 2 - 90, h / 2 - 6, -1, false
            )
        }
    }
}
