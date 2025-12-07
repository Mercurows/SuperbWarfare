package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.overlay.components.CENTER
import com.atsuishio.superbwarfare.client.overlay.components.StringComponent
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object MortarInfoOverlay : CommonOverlay("mortar_info") {

    val pitch = StringComponent(CENTER.offset(-90F, -26F))
    val yaw = StringComponent(CENTER.offset(-90F, -16F))
    val range = StringComponent(CENTER.offset(-90F, -6F))

    override fun RenderContext.renderOverlay() {
        val lookingEntity = TraceTool.findLookingEntity(player, 6.0)

        if (lookingEntity is MortarEntity) {
            pitch.render(
                this,
                Component.translatable("tips.superbwarfare.mortar.pitch")
                        + format1D(-lookingEntity.xRot.toDouble(), "°")
            )

            yaw.render(
                this,
                Component.translatable("tips.superbwarfare.mortar.yaw")
                        + format1D(lookingEntity.yRot.toDouble(), "°")
            )

            range.render(
                this,
                Component.translatable("tips.superbwarfare.mortar.range") + format1D(
                    getRange(
                        -lookingEntity.xRot.toDouble(),
                        lookingEntity.getProjectileVelocity("Main").toDouble(),
                        lookingEntity.getProjectileGravity("Main").toDouble()
                    ).toInt().toDouble(), "m"
                )
            )
        }
    }
}
