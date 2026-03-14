package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.misc.firingParameters
import com.atsuishio.superbwarfare.network.message.send.FiringParametersEditMessage
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
open class ArtilleryIndicatorScreen(private val stack: ItemStack, private val hand: InteractionHand) :
    Screen(Component.translatable("item.superbwarfare.artillery_indicator")) {
    lateinit var posX: EditBox
    lateinit var posY: EditBox
    lateinit var posZ: EditBox
    lateinit var radius: EditBox

    var isDepressed: Boolean = false

    private var init = false

    protected var imageWidth: Int = 176
    protected var imageHeight: Int = 84

    init {
        if (!stack.isEmpty) {
            this.isDepressed = stack.firingParameters.isDepressed
        }
    }

    override fun tick() {
        super.tick()
        if (!this.init) {
            if (!this.stack.isEmpty) {
                val parameters = stack.firingParameters
                val pos = parameters.pos
                this.posX.value = "${pos.x}"
                this.posY.value = "${pos.y}"
                this.posZ.value = "${pos.z}"
                this.radius.value = "${max(0, parameters.radius)}"
            }
            this.init = true
        }
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        this.renderBg(pGuiGraphics, pMouseX, pMouseY)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.renderPositions(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    protected fun renderPositions(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        val poseStack = pGuiGraphics.pose()

        poseStack.pushPose()

        this.posX.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.posY.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.posZ.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.radius.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)

        poseStack.popPose()
    }

    protected fun renderBg(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256)

        if (pMouseX >= i + 98 && pMouseX <= i + 162 && pMouseY >= j + 19 && pMouseY <= j + 49) {
            pGuiGraphics.renderTooltip(
                this.font,
                if (this.isDepressed) Component.translatable("tips.superbwarfare.mortar.target_pos.depressed_trajectory")
                    .withStyle(ChatFormatting.WHITE) else Component.translatable("tips.superbwarfare.mortar.target_pos.lofted_trajectory")
                    .withStyle(ChatFormatting.WHITE),
                pMouseX, pMouseY
            )
        }

        pGuiGraphics.drawString(this.font, this.title, i + 6, j + 6, 4210752, false)
    }

    override fun init() {
        super.init()
        this.subInit()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val modeButton = this.ModeButton(i + 99, j + 19, 64, 30)
        this.addRenderableWidget(modeButton)

        val doneButton = this.DoneButton(i + 113, j + 54, 48, 15)
        this.addRenderableWidget(doneButton)
    }

    protected fun subInit() {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        this.posX = EditBox(this.font, i + 24, j + 20, 60, 12, Component.empty())
        this.initEditBox(this.posX)

        this.posY = EditBox(this.font, i + 24, j + 33, 60, 12, Component.empty())
        this.initEditBox(this.posY)

        this.posZ = EditBox(this.font, i + 24, j + 46, 60, 12, Component.empty())
        this.initEditBox(this.posZ)

        this.radius = EditBox(this.font, i + 24, j + 59, 20, 12, Component.empty())
        this.initEditBox(this.radius)
        this.radius.setMaxLength(2)
        this.radius.setFilter { it.matches("\\d*".toRegex()) }
    }

    protected fun initEditBox(editBox: EditBox) {
        editBox.setCanLoseFocus(true)
        editBox.setTextColor(-1)
        editBox.setTextColorUneditable(-1)
        editBox.setBordered(false)
        editBox.setMaxLength(9)
        this.addWidget(editBox)
        editBox.setEditable(true)
        editBox.setFilter { it.matches("-?\\d*".toRegex()) }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class ModeButton(pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            this@ArtilleryIndicatorScreen.isDepressed = !this@ArtilleryIndicatorScreen.isDepressed
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            val isDepressed = this@ArtilleryIndicatorScreen.isDepressed
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                if (isDepressed) this.y + 14 else this.y,
                177f,
                (if (isDepressed) 62 else 33).toFloat(),
                61,
                if (isDepressed) 14 else 28,
                256,
                256
            )
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class DoneButton(pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            if (!this@ArtilleryIndicatorScreen.init) return
            if (this@ArtilleryIndicatorScreen.minecraft != null) {
                this@ArtilleryIndicatorScreen.minecraft!!.setScreen(null)
            }
            sendPacketToServer(
                FiringParametersEditMessage(
                    getEditBoxValue(this@ArtilleryIndicatorScreen.posX.value),
                    getEditBoxValue(this@ArtilleryIndicatorScreen.posY.value),
                    getEditBoxValue(this@ArtilleryIndicatorScreen.posZ.value),
                    max(0, getEditBoxValue(this@ArtilleryIndicatorScreen.radius.value)),
                    this@ArtilleryIndicatorScreen.isDepressed,
                    this@ArtilleryIndicatorScreen.hand == InteractionHand.MAIN_HAND
                )
            )
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                this.y,
                177f,
                (if (this.isHovered) 16 else 0).toFloat(),
                48,
                15,
                256,
                256
            )
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }

        fun getEditBoxValue(value: String): Int {
            if (value == "-") return 0
            return try {
                value.toInt()
            } catch (_: NumberFormatException) {
                0
            }
        }
    }

    companion object {
        private val TEXTURE = loc("textures/gui/artillery_indicator.png")
    }
}
