package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.curio.DogTagItem.Companion.getColors
import com.atsuishio.superbwarfare.network.message.send.DogTagFinishEditMessage
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.SharedConstants
import net.minecraft.Util
import net.minecraft.client.GameNarrator
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

@OnlyIn(Dist.CLIENT)
open class DogTagEditorScreen(var stack: ItemStack, private val hand: InteractionHand) : Screen(GameNarrator.NO_TITLE) {
    lateinit var name: EditBox
    private var currentColor: Short = 0
    private var icon: Array<ShortArray> = Array(16) { ShortArray(16) }

    private var init = false

    protected var imageWidth: Int = 207
    protected var imageHeight: Int = 185

    private var itemName: String? = null

    override fun isPauseScreen(): Boolean {
        return false
    }

    protected fun renderBg(pGuiGraphics: GuiGraphics) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256)

        pGuiGraphics.renderItem(stack, i + 18, j + 36)

        val pose = pGuiGraphics.pose()

        pose.pushPose()

        for (x in this.icon.indices) {
            for (y in this.icon.indices) {
                val num = this.icon[x][y]
                if (num.toInt() != -1) {
                    pGuiGraphics.fill(
                        i + 66 + x * 9, j + 44 + y * 9, i + 58 + x * 9, j + 36 + y * 9,
                        getColorByNum(num)
                    )
                }
            }
        }

        pose.popPose()
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        this.renderBg(pGuiGraphics)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        this.name.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        this.drawColor(pMouseX, pMouseY, pButton)
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        this.drawColor(pMouseX, pMouseY, pButton)
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
    }

    private fun drawColor(pMouseX: Double, pMouseY: Double, pButton: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        if (pMouseX >= i + 57 && pMouseX <= i + 201 && pMouseY >= j + 36 && pMouseY <= j + 179) {
            val posX = pMouseX - i - 57
            val posY = pMouseY - j - 36
            if (ceil(posX) % 9 == 0.0 || ceil(posY) % 9 == 0.0) return

            val x = floor(posX / 9).toInt()
            val y = floor(posY / 9).toInt()

            this.icon[x.coerceIn(0, 15)][y.coerceIn(0, 15)] = if (pButton == 0) this.currentColor else -1
        }
    }

    override fun tick() {
        super.tick()
        this.name.tick()
        if (!this.init) {
            if (!this.stack.isEmpty) {
                this.name.value = this.stack.getHoverName().string
                this.icon = getColors(this.stack)
            }
            this.init = true
        }
    }

    override fun init() {
        super.init()

        this.subInit()

        this.clearColors()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        for (k in 0..15) {
            val button = ColorButton(k.toShort(), i + 6 + (k % 2) * 22, j + 62 + (k / 2) * 10, 18, 8)
            this.addRenderableWidget(button)
        }
        val eraserButton = ColorButton((-1).toShort(), i + 17, j + 143, 18, 8)
        this.addRenderableWidget(eraserButton)

        val finishButton = FinishButton(i + 6, j + 167, 40, 13)
        this.addRenderableWidget(finishButton)
    }

    protected fun subInit() {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        this.name = EditBox(this.font, i + 9, j + 11, 180, 12, Component.empty())
        this.name.setCanLoseFocus(false)
        this.name.setTextColor(-1)
        this.name.setTextColorUneditable(-1)
        this.name.setBordered(false)
        this.name.setMaxLength(30)
        this.name.setResponder { this.onNameChanged(it) }
        this.addWidget(this.name)
        this.name.setEditable(true)
    }

    private fun onNameChanged(name: String) {
        var s = name
        if (!stack.hasCustomHoverName() && name == stack.getHoverName().string) {
            s = ""
        }

        if (this.setItemName(s)) {
            val player = localPlayer ?: return
            sendPacketTo(player, ServerboundRenameItemPacket(s))
        }
    }

    fun clearColors() {
        for (el in this.icon) {
            Arrays.fill(el, (-1).toShort())
        }
    }

    fun setItemName(pItemName: String): Boolean {
        val s = validateName(pItemName)
        if (s != null && s != this.itemName) {
            this.itemName = s
            if (!this.stack.isEmpty) {
                if (Util.isBlank(s)) {
                    this.stack.resetHoverName()
                } else {
                    this.stack.setHoverName(Component.literal(s))
                }
            }
            return true
        } else {
            return false
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class ColorButton(var color: Short, pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            this@DogTagEditorScreen.currentColor = this.color
            if (this.color.toInt() == -1 && hasShiftDown()) {
                this@DogTagEditorScreen.clearColors()
            }
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            if (this.isHovered || this@DogTagEditorScreen.currentColor == this.color) {
                if (this.color.toInt() == -1) {
                    pGuiGraphics.blit(
                        TEXTURE, this.x, this.y, 19f, 186f,
                        18, 8, 256, 256
                    )
                } else {
                    pGuiGraphics.blit(
                        TEXTURE, this.x, this.y, 0f, 186f,
                        18, 8, 256, 256
                    )
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal open inner class FinishButton(pX: Int, pY: Int, pWidth: Int, pHeight: Int) :
        AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {
        override fun onPress() {
            if (!this@DogTagEditorScreen.init) return
            if (this@DogTagEditorScreen.minecraft != null) {
                this@DogTagEditorScreen.minecraft!!.setScreen(null)
            }
            this.updateLocal(this@DogTagEditorScreen.icon, this@DogTagEditorScreen.name.value)
            sendPacketToServer(DogTagFinishEditMessage(
                this@DogTagEditorScreen.icon, this@DogTagEditorScreen.name.value,
                this@DogTagEditorScreen.hand == InteractionHand.MAIN_HAND
            ))
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            if (this.isHovered) {
                pGuiGraphics.blit(
                    TEXTURE, this.x, this.y, 0f, 195f,
                    40, 13, 256, 256
                )
            }
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }

        protected fun updateLocal(colors: Array<ShortArray>, name: String) {
            val colorsTag = CompoundTag()
            for (i in colors.indices) {
                val color = IntArray(colors[i].size)
                for (j in colors[i].indices) {
                    color[j] = colors[i][j].toInt()
                }
                colorsTag.putIntArray("Color$i", color)
            }
            this@DogTagEditorScreen.stack.getOrCreateTag().put("Colors", colorsTag)

            if (!name.isEmpty()) {
                this@DogTagEditorScreen.stack.setHoverName(Component.literal(name))
            }
        }
    }

    companion object {
        private val TEXTURE = loc("textures/gui/dog_tag_editor.png")

        private fun validateName(pItemName: String): String? {
            val s = SharedConstants.filterText(pItemName)
            return if (s.length <= 30) s else null
        }

        fun getColorByNum(num: Short): Int {
            return when (num.toInt()) {
                0 -> -0x1000000
                1 -> -0x1
                2 -> -0x7f7f80
                3 -> -0x2bdbdc
                4 -> -0x5600
                5 -> -0x100
                6 -> -0xc31fc4
                7 -> -0x993301
                8 -> -0xc5b001
                9 -> -0x49ab01
                10 -> -0x82a7bf
                11 -> -0x6859
                12 -> -0x896ba2
                13 -> -0x3c00
                14 -> -0xb3bda5
                15 -> -0x71b30
                else -> -1
            }
        }
    }
}
