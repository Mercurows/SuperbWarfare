package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineLabel
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.loading.LoadingModList
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

@OnlyIn(Dist.CLIENT)
class SnapshotWarningScreen(val lastScreen: Screen) : Screen(
    Component.translatable("warning.superbwarfare.title.snapshot").withStyle(ChatFormatting.BOLD)
) {
    private var message: MultiLineLabel = MultiLineLabel.EMPTY
    private var freezeTicks = 100
    private var button: AbstractButton? = null

    fun initButtons(yOffset: Int) {
        this.button = this.createProceedButton(yOffset)
        this.addRenderableWidget(this.button!!)
        this.button?.active = false
    }

    override fun init() {
        super.init()
        this.message = MultiLineLabel.create(
            this.font,
            Component.translatable("warning.superbwarfare.content.snapshot"),
            this.width - 100
        )
        val i: Int = (this.message.lineCount + 1) * 18

        this.initButtons(i)
    }

    override fun tick() {
        super.tick()
        this.button?.message = Component.translatable("gui.proceed").append(" (${this.freezeTicks / 20}s)")

        if (freezeTicks > 0) {
            --freezeTicks
        } else {
            this.button?.active = true
        }
    }

    private fun createProceedButton(yOffset: Int): AbstractButton {
        return Button.builder(Component.translatable("gui.proceed").append(" (${this.freezeTicks / 20}s)")) {
            mc.setScreen(this.lastScreen)
        }.bounds(this.width / 2 - 75, 100 + yOffset, 150, 20).build()
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        pGuiGraphics.drawString(this.font, this.title, (this.width - this.font.width(this.title)) / 2, 30, 16777215)
        val i = this.width / 2 - this.message.width / 2
        this.message.renderLeftAligned(pGuiGraphics, i, 70, 18, 16777215)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    override fun onClose() {
        mc.setScreen(this.lastScreen)
    }

    @Mod.EventBusSubscriber(value = [Dist.CLIENT])
    companion object {
        var firstTimeStart = false

        @SubscribeEvent(priority = EventPriority.HIGH)
        fun onTitleScreenOpen(event: ScreenEvent.Init.Post) {
            if (!FMLEnvironment.production) return
            if (firstTimeStart || event.screen !is TitleScreen) return
            val version = getVersion() ?: return
            if (!version.toString().lowercase().contains("snapshot")) return

            mc.setScreen(SnapshotWarningScreen(event.screen))
            firstTimeStart = true
        }

        fun getVersion(): DefaultArtifactVersion? {
            val modFile = LoadingModList.get().getModFileById(com.atsuishio.superbwarfare.Mod.MODID) ?: return null
            return DefaultArtifactVersion(modFile.versionString())
        }
    }
}