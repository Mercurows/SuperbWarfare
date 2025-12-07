package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

object MortarInfoOverlay : IGuiOverlay {
    const val ID: String = Mod.MODID + "_mortar_info"

    override fun render(
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val player: Player? = gui.getMinecraft().player
        var lookingEntity: Entity? = null
        if (player != null) {
            lookingEntity = TraceTool.findLookingEntity(player, 6.0)
        }
        if (lookingEntity is MortarEntity) {
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.pitch")
                    .append(Component.literal(format1D(-lookingEntity.getXRot().toDouble(), "°"))),
                screenWidth / 2 - 90, screenHeight / 2 - 26, -1, false
            )
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.yaw")
                    .append(Component.literal(format1D(lookingEntity.getYRot().toDouble(), "°"))),
                screenWidth / 2 - 90, screenHeight / 2 - 16, -1, false
            )
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.range")
                    .append(
                        Component.literal(
                            format1D(
                                getRange(
                                    -lookingEntity.getXRot().toDouble(),
                                    lookingEntity.getProjectileVelocity("Main").toDouble(),
                                    lookingEntity.getProjectileGravity("Main").toDouble()
                                ).toInt().toDouble(), "m"
                            )
                        )
                    ),
                screenWidth / 2 - 90, screenHeight / 2 - 6, -1, false
            )
        }
    }
}
