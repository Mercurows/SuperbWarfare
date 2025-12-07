package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class RenderContext(val guiGraphics: GuiGraphics, val deltaTracker: DeltaTracker) {
    val screenWidth get() = guiGraphics.guiWidth()
    val screenHeight get() = guiGraphics.guiHeight()

    val w by ::screenWidth
    val h by ::screenHeight

    val player by ::localPlayer
    val camera: Camera get() = mc.gameRenderer.mainCamera
    val cameraPos: Vec3 get() = camera.position

    val isFirstPerson get() = options.cameraType.isFirstPerson

    val partialTick get() = deltaTracker.getGameTimeDeltaPartialTick(true)
}

@OnlyIn(Dist.CLIENT)
abstract class CommonOverlay(id: String) : LayeredDraw.Layer {
    val overlayID = loc(id)

    abstract fun RenderContext.renderOverlay()

    open fun shouldRender() = !options.hideGui && localPlayer != null && !(localPlayer?.isSpectator ?: true)

    override fun render(
        guiGraphics: GuiGraphics,
        deltaTracker: DeltaTracker
    ) {
        if (!shouldRender()) return

        RenderContext(guiGraphics, deltaTracker).renderOverlay()
    }
}