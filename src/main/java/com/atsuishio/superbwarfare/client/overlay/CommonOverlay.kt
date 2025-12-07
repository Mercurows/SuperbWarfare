package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.Camera
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.gui.overlay.ForgeGui
import net.minecraftforge.client.gui.overlay.IGuiOverlay

@OnlyIn(Dist.CLIENT)
class RenderContext(
    val gui: ForgeGui,
    val guiGraphics: GuiGraphics,
    val partialTick: Float,
    val screenWidth: Int,
    val screenHeight: Int
) {
    val w by ::screenWidth
    val h by ::screenHeight

    val player by lazy { localPlayer }
    val camera: Camera get() = mc.gameRenderer.mainCamera
    val cameraPos: Vec3 get() = camera.position

    val isFirstPerson get() = options.cameraType.isFirstPerson
}

@OnlyIn(Dist.CLIENT)
abstract class CommonOverlay(id: String) : IGuiOverlay {
    val overlayID = Mod.MODID + "_" + id

    abstract fun RenderContext.renderOverlay()

    open fun shouldRender() = !options.hideGui && localPlayer != null && !(localPlayer?.isSpectator ?: true)

    override fun render(
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (!shouldRender()) return

        RenderContext(gui, guiGraphics, partialTick, screenWidth, screenHeight).renderOverlay()
    }
}