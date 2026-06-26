package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.send.LoiterConfigMessage
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.GameNarrator
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.lwjgl.glfw.GLFW

/**
 * 自动绕点盘旋配置 GUI
 *
 * 布局参考 ArtilleryIndicatorScreen (176x84)，
 * 左侧 X/Y/Z/R 输入框位置与其一致，
 * 右侧为 Toggle / Confirm 按钮和跳过地形勾选框。
 * 暂时使用色块绘制，后续替换为贴图。
 */
@OnlyIn(Dist.CLIENT)
class LoiterConfigScreen(private val vehicle: VehicleEntity) :
    Screen(GameNarrator.NO_TITLE) {

    // ==================== Layout ====================

    private val imageWidth = 176
    private val imageHeight = 84

    // ==================== Widgets ====================

    private lateinit var editX: EditBox
    private lateinit var editY: EditBox
    private lateinit var editZ: EditBox
    private lateinit var editR: EditBox

    private var active = false
    private var skipTerrain = false

    private lateinit var terrainCheckbox: TerrainCheckbox
    private lateinit var toggleButton: ToggleButton

    private var initDone = false

    // ==================== Screen overrides ====================

    override fun isPauseScreen(): Boolean = false

    override fun init() {
        super.init()

        val i = (this.width - imageWidth) / 2
        val j = (this.height - imageHeight) / 2

        // EditBoxes — positions match ArtilleryIndicatorScreen exactly
        editX = EditBox(this.font, i + 24, j + 20, 60, 12, Component.empty())
        initEditBox(editX)

        editY = EditBox(this.font, i + 24, j + 33, 60, 12, Component.empty())
        initEditBox(editY)

        editZ = EditBox(this.font, i + 24, j + 46, 60, 12, Component.empty())
        initEditBox(editZ)

        editR = EditBox(this.font, i + 24, j + 59, 60, 12, Component.empty())
        initEditBox(editR)
        editR.setMaxLength(5)
        editR.setFilter { it.matches("\\d*".toRegex()) } // digits only, 200~10000

        // Right-side buttons (toggle at texture position 98,19 size 51x24)
        toggleButton = ToggleButton(i + 97, j + 18, 51, 24)
        this.addRenderableWidget(toggleButton)
        this.addRenderableWidget(ConfirmButton(i + 97, j + 45, 51, 24))
        // Checkbox: 15x15 at texture position (153,54), textures at (229,0)/(229,16)
        terrainCheckbox = TerrainCheckbox(i + 153, j + 54, 15)
        this.addRenderableWidget(terrainCheckbox)
    }

    override fun tick() {
        // Auto-close if player leaves the vehicle
        if (this.minecraft?.player?.vehicle !== vehicle || vehicle.isRemoved) {
            this.onClose()
            return
        }

        if (!initDone) {
            seedFromVehicle()
            initDone = true
        }

        super.tick()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.renderBackground(guiGraphics)

        val i = (this.width - imageWidth) / 2
        val j = (this.height - imageHeight) / 2

        // --- Panel background (texture) ---
        guiGraphics.blit(TEXTURE, i, j, 0f, 0f, imageWidth, imageHeight, 256, 256)

        // --- Title ---
        val title = Component.translatable("container.superbwarfare.loiter_config.title")
        guiGraphics.drawString(this.font, title, i + 6, j + 6, 0xFF000000.toInt(), false)

        // Render buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        // Render EditBoxes manually (matching ArtilleryIndicatorScreen.renderPositions)
        editX.render(guiGraphics, mouseX, mouseY, partialTick)
        editY.render(guiGraphics, mouseX, mouseY, partialTick)
        editZ.render(guiGraphics, mouseX, mouseY, partialTick)
        editR.render(guiGraphics, mouseX, mouseY, partialTick)

        // Hover overlay for toggle button (confirmation prompt + tooltip)
        if (toggleButton.isHovered) {
            guiGraphics.blit(TEXTURE, toggleButton.x, toggleButton.y, 177f, 100f, 51, 24, 256, 256)
            guiGraphics.renderTooltip(
                this.font,
                Component.translatable(if (active) "screen.superbwarfare.loiter_config.loiter_on" else "screen.superbwarfare.loiter_config.loiter_off"),
                mouseX, mouseY
            )
        }

        // Hover overlay for checkbox (prompt texture + tooltip)
        if (terrainCheckbox.isHovered) {
            guiGraphics.blit(TEXTURE, terrainCheckbox.x, terrainCheckbox.y, 229f, 0f, 15, 15, 256, 256)
            guiGraphics.renderTooltip(
                this.font,
                Component.translatable("container.superbwarfare.loiter_config.skip_terrain"),
                mouseX, mouseY
            )
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Enter = Confirm (send + close)
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            sendLoiterConfigToServer()
            this.onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    // ==================== Helpers ====================

    private fun initEditBox(editBox: EditBox) {
        editBox.setCanLoseFocus(true)
        editBox.setTextColor(-1)
        editBox.setTextColorUneditable(-1)
        editBox.setBordered(false)
        editBox.setMaxLength(9)
        this.addWidget(editBox)
        editBox.setEditable(true)
        editBox.setFilter { it.matches("-?\\d*".toRegex()) }
    }

    private fun seedFromVehicle() {
        editX.value = vehicle.loiterCenterX.toInt().toString()
        editY.value = vehicle.loiterCenterY.toInt().toString()
        editZ.value = vehicle.loiterCenterZ.toInt().toString()
        editR.value = vehicle.loiterRadius.toInt().toString()
        active = vehicle.loiterActive
    }

    private fun sendLoiterConfigToServer() {
        val x = editX.value.toFloatOrNull() ?: return
        val y = editY.value.toFloatOrNull() ?: return
        val z = editZ.value.toFloatOrNull() ?: return
        val r = editR.value.toFloatOrNull() ?: return

        sendPacketToServer(
            LoiterConfigMessage(
                centerX = x,
                centerY = y,
                centerZ = z,
                radius = r,
                active = active,
                skipTerrain = skipTerrain
            )
        )
    }

    // ==================== Drawing ====================

    private fun drawRectBorder(
        guiGraphics: GuiGraphics,
        left: Int, top: Int, width: Int, height: Int,
        color: Int
    ) {
        guiGraphics.fill(left, top, left + width, top + 1, color)
        guiGraphics.fill(left, top + height - 1, left + width, top + height, color)
        guiGraphics.fill(left, top, left + 1, top + height, color)
        guiGraphics.fill(left + width - 1, top, left + width, top + height, color)
    }

    // ==================== Inner Button Classes ====================

    @OnlyIn(Dist.CLIENT)
    private inner class ToggleButton(x: Int, y: Int, width: Int, height: Int) :
        AbstractButton(x, y, width, height, Component.empty()) {

        override fun onPress() {
            this@LoiterConfigScreen.active = !this@LoiterConfigScreen.active
            this@LoiterConfigScreen.sendLoiterConfigToServer()
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            val on = this@LoiterConfigScreen.active
            // ON: (177,0)  OFF: (177,25)  each 51x24
            val vOffset = if (on) 0 else 25
            guiGraphics.blit(TEXTURE, this.x, this.y, 177f, vOffset.toFloat(), 51, 24, 256, 256)
        }

        override fun updateWidgetNarration(narration: NarrationElementOutput) {}
    }

    @OnlyIn(Dist.CLIENT)
    private inner class ConfirmButton(x: Int, y: Int, width: Int, height: Int) :
        AbstractButton(x, y, width, height, Component.empty()) {

        override fun onPress() {
            this@LoiterConfigScreen.sendLoiterConfigToServer()
            this@LoiterConfigScreen.onClose()
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            // Normal: (178,50)  Hover: (178,75)  each 51x24
            val vOffset = if (this.isHovered) 75 else 50
            guiGraphics.blit(TEXTURE, this.x, this.y, 177f, vOffset.toFloat(), 51, 24, 256, 256)
        }

        override fun updateWidgetNarration(narration: NarrationElementOutput) {}
    }

    @OnlyIn(Dist.CLIENT)
    private inner class TerrainCheckbox(x: Int, y: Int, size: Int) :
        AbstractButton(x, y, size, size, Component.empty()) {

        override fun onPress() {
            this@LoiterConfigScreen.skipTerrain = !this@LoiterConfigScreen.skipTerrain
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
            // Only render checked state here; hover prompt is drawn as overlay in render()
            if (this@LoiterConfigScreen.skipTerrain) {
                guiGraphics.blit(TEXTURE, this.x, this.y, 229f, 16f, 15, 15, 256, 256)
            }
        }

        override fun updateWidgetNarration(narration: NarrationElementOutput) {}
    }

    companion object {
        private val TEXTURE = loc("textures/gui/loiter_config.png")
    }
}
