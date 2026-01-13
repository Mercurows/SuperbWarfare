package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.overlay.components.BaseComponent
import com.atsuishio.superbwarfare.tools.isNullOrSpector
import com.atsuishio.superbwarfare.tools.localPlayer
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
    var gui: ForgeGui,
    var guiGraphics: GuiGraphics,
    var partialTick: Float,
    var screenWidth: Int,
    var screenHeight: Int
) {
    val w by ::screenWidth
    val h by ::screenHeight

    // Non-null local player, MUST BE USED AFTER NULL CHECK!
    val player get() = localPlayer!!

    val mc get() = com.atsuishio.superbwarfare.tools.mc

    val camera: Camera get() = mc.gameRenderer.mainCamera
    val cameraPos: Vec3 get() = camera.position

    val isFirstPerson get() = options.cameraType.isFirstPerson

    val deltaFrame by ::partialTick
}

@OnlyIn(Dist.CLIENT)
abstract class CommonOverlay(id: String) : IGuiOverlay {
    val ID = Mod.MODID + "_" + id

    val components = mutableListOf<BaseComponent>()

    fun registerComponents(vararg components: BaseComponent) {
        this.components.addAll(components)
    }

    open fun RenderContext.preRender() {}

    open fun RenderContext.render() {
        components.forEach {
            if (it.shouldRender()) {
                it.apply { renderComponent() }
            }
        }
    }

    open fun shouldRender() = !options.hideGui && !localPlayer.isNullOrSpector()

    private lateinit var context: RenderContext

    override fun render(
        gui: ForgeGui,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (!shouldRender()) return

        if (!this::context.isInitialized) {
            context = RenderContext(gui, guiGraphics, partialTick, screenWidth, screenHeight)
        } else {
            context.gui = gui
            context.guiGraphics = guiGraphics
            context.partialTick = partialTick
            context.screenWidth = screenWidth
            context.screenHeight = screenHeight
        }

        with(context) {
            preRender()
            render()
        }
    }
}