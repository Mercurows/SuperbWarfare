package com.atsuishio.superbwarfare.client.tooltip

import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.atsuishio.superbwarfare.client.tooltip.component.DogTagImageComponent
import com.atsuishio.superbwarfare.item.curio.DogTagItem.Companion.getColors
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.world.item.ItemStack

open class ClientDogTagImageTooltip(tooltip: DogTagImageComponent) : ClientTooltipComponent {
    protected val tipWidth: Int = tooltip.width
    protected val tipHeight: Int = tooltip.height
    protected val stack: ItemStack = tooltip.stack

    override fun renderImage(pFont: Font, pX: Int, pY: Int, pGuiGraphics: GuiGraphics) {
        if (this.stack.tag == null) return
        val colors = getColors(this.stack)
        if (isAllMinusOne(colors)) return

        pGuiGraphics.pose().pushPose()

        for (i in 0..15) {
            for (j in 0..15) {
                if (colors[i][j].toInt() == -1) continue
                pGuiGraphics.fill(
                    5 + pX + i * 4 + 4, 5 + pY + j * 4 + 4, 5 + pX + i * 4, 5 + pY + j * 4,
                    DogTagEditorScreen.getColorByNum(colors[i][j])
                )
            }
        }

        pGuiGraphics.pose().popPose()
    }

    override fun getHeight(): Int {
        return if (!shouldRenderIcon(this.stack)) 0 else this.tipHeight
    }

    override fun getWidth(pFont: Font): Int {
        return if (!shouldRenderIcon(this.stack)) 0 else this.tipWidth
    }

    companion object {
        fun shouldRenderIcon(stack: ItemStack): Boolean {
            if (stack.tag == null) return false
            val colors = getColors(stack)
            return !isAllMinusOne(colors)
        }

        fun isAllMinusOne(arr: Array<ShortArray>): Boolean {
            for (row in arr) {
                for (element in row) {
                    if (element != (-1).toShort()) {
                        return false
                    }
                }
            }
            return true
        }
    }
}
