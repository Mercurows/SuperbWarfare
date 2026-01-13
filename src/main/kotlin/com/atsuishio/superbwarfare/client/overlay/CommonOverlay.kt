package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.overlay.components.BaseComponent
import com.atsuishio.superbwarfare.tools.isNullOrSpector
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class RenderContext(var guiGraphics: GuiGraphics, var deltaTracker: DeltaTracker) {
    val screenWidth get() = guiGraphics.guiWidth()
    val screenHeight get() = guiGraphics.guiHeight()

    val w by ::screenWidth
    val h by ::screenHeight

    // Non-null local player, MUST BE USED AFTER NULL CHECK!
    val player get() = localPlayer!!

    val mc get() = com.atsuishio.superbwarfare.tools.mc

    val camera: Camera get() = mc.gameRenderer.mainCamera
    val cameraPos: Vec3 get() = camera.position

    val isFirstPerson get() = options.cameraType.isFirstPerson

    val partialTick get() = deltaTracker.getGameTimeDeltaPartialTick(true)
    val deltaFrame by ::partialTick
}

@OnlyIn(Dist.CLIENT)
abstract class CommonOverlay(id: String) : LayeredDraw.Layer {
    val ID = loc(id)

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
        guiGraphics: GuiGraphics,
        deltaTracker: DeltaTracker
    ) {
        if (!shouldRender()) return

        if (!this::context.isInitialized) {
            context = RenderContext(guiGraphics, deltaTracker)
        } else {
            context.guiGraphics = guiGraphics
            context.deltaTracker = deltaTracker
        }

        with(context) {
            preRender()
            render()
        }
    }
}