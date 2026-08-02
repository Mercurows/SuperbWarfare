package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object DecoyOverlayHelper {

    @JvmStatic
    fun renderThirdPersonDecoyInfo(entity: VehicleEntity, guiGraphics: GuiGraphics, x: Int, y: Int, color: Int) {
        val font = mc.font
        if (entity.hasDecoy()) {
            val key = if (entity.hasSmokeDecoy()) "smoke" else "flare"

            if (entity.decoyCount > 0) {
                guiGraphics.drawString(
                    font,
                    Component.translatable("tips.superbwarfare.$key.ready").append(
                        Component.literal(" ${entity.decoyCount} [${ModKeyMappings.RELEASE_DECOY.key.displayName.string}]")
                    ),
                    x,
                    y,
                    color,
                    false
                )
            } else {
                if (entity.decoyItemCount > 0) {
                    guiGraphics.drawString(
                        font,
                        Component.translatable("tips.superbwarfare.$key.reloading"),
                        x,
                        y,
                        0xFF0000,
                        false
                    )
                } else {
                    guiGraphics.drawString(
                        font,
                        Component.translatable("tips.superbwarfare.$key.none"),
                        x,
                        y,
                        0xFF0000,
                        false
                    )
                }
            }
        }
    }

    @JvmStatic
    fun renderFirstPersonDecoyInfo(entity: VehicleEntity, guiGraphics: GuiGraphics, y: Int, color: Int) {
        val font = mc.font
        if (entity.hasDecoy()) {
            val key = if (entity.hasSmokeDecoy()) "smoke" else "flare"

            if (entity.decoyCount > 0) {
                val componentReady = Component.translatable("tips.superbwarfare.$key.ready").append(
                    Component.literal(" ${entity.decoyCount} [${ModKeyMappings.RELEASE_DECOY.key.displayName.string}]")
                )
                val length = font.width(componentReady)

                guiGraphics.drawString(
                    font,
                    componentReady,
                    -length / 2,
                    y,
                    color,
                    false
                )
            } else {
                val componentReloading = if (entity.decoyItemCount < 1) {
                    Component.translatable("tips.superbwarfare.$key.none")
                } else {
                    Component.translatable("tips.superbwarfare.$key.reloading")
                }
                val length = font.width(componentReloading)

                guiGraphics.drawString(
                    font,
                    componentReloading,
                    -length / 2,
                    y,
                    0xFF0000,
                    false
                )
            }
        }
    }
}