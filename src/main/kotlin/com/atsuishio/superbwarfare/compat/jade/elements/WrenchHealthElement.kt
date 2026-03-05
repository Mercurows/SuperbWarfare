package com.atsuishio.superbwarfare.compat.jade.elements

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.Vec2
import snownee.jade.api.Identifiers
import snownee.jade.api.theme.IThemeHelper
import snownee.jade.api.ui.Element
import snownee.jade.impl.config.PluginConfig
import snownee.jade.overlay.DisplayHelper
import snownee.jade.overlay.OverlayRenderer
import kotlin.math.ceil

class WrenchHealthElement(maxHealth: Float, health: Float) : Element() {
    private val text: String

    init {
        var maxHealth = maxHealth
        var health = health
        if (!PluginConfig.INSTANCE.get(Identifiers.MC_ENTITY_HEALTH_SHOW_FRACTIONS)) {
            maxHealth = ceil(maxHealth)
            health = ceil(health)
        }

        this.text = "  ${DisplayHelper.dfCommas.format(health.toDouble())}/${DisplayHelper.dfCommas.format(maxHealth.toDouble())}"
    }

    override fun getSize(): Vec2 {
        val font = Minecraft.getInstance().font
        return Vec2(8f + font.width(this.text), 10f)
    }

    override fun render(guiGraphics: GuiGraphics?, x: Float, y: Float, maxX: Float, maxY: Float) {
        RenderSystem.setShaderColor(1f, 1f, 1f, OverlayRenderer.alpha)
        RenderSystem.setShaderTexture(0, WRENCH_ICON)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        // 扳手图标
        RenderHelper.preciseBlit(guiGraphics, WRENCH_ICON, x + 2, y, 0f, 0f, 8f, 8f, 8f, 8f)
        // 文字
        DisplayHelper.INSTANCE.drawText(guiGraphics, this.text, x + 6, y, IThemeHelper.get().normalColor)

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    companion object {
        private val WRENCH_ICON = loc("textures/overlay/vehicle/jade/vehicle_health.png")
    }
}
