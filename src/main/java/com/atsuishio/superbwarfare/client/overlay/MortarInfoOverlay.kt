package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.font
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import javax.annotation.ParametersAreNonnullByDefault

@OnlyIn(Dist.CLIENT)
object MortarInfoOverlay : LayeredDraw.Layer {
    @JvmField
    val ID: ResourceLocation = loc("mortar_info")

    @ParametersAreNonnullByDefault
    override fun render(guiGraphics: GuiGraphics, deltaTracker: DeltaTracker) {
        if (options.hideGui) return
        val w = guiGraphics.guiWidth()
        val h = guiGraphics.guiHeight()
        val player = localPlayer ?: return
        val lookingEntity = TraceTool.findLookingEntity(player, 6.0)

        if (lookingEntity is MortarEntity) {
            guiGraphics.drawString(
                font, Component.translatable("tips.superbwarfare.mortar.pitch")
                    .append(Component.literal(format1D(-lookingEntity.xRot.toDouble(), "°"))),
                w / 2 - 90, h / 2 - 26, -1, false
            )
            guiGraphics.drawString(
                font, Component.translatable("tips.superbwarfare.mortar.yaw")
                    .append(Component.literal(format1D(lookingEntity.yRot.toDouble(), "°"))),
                w / 2 - 90, h / 2 - 16, -1, false
            )
            guiGraphics.drawString(
                font, Component.translatable("tips.superbwarfare.mortar.range")
                    .append(
                        Component.literal(
                            format1D(
                                getRange(
                                    -lookingEntity.xRot.toDouble(),
                                    lookingEntity.getProjectileVelocity("Main").toDouble(),
                                    lookingEntity.getProjectileGravity("Main").toDouble()
                                ).toInt().toDouble(), "m"
                            )
                        )
                    ),
                w / 2 - 90, h / 2 - 6, -1, false
            )
        }
    }
}
