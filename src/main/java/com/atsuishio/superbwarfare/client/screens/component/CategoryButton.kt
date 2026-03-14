package com.atsuishio.superbwarfare.client.screens.component

import com.atsuishio.superbwarfare.client.screens.VehicleAssemblingScreen
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component

class CategoryButton(x: Int, y: Int, var category: VehicleAssemblingRecipe.Category, onPress: OnPress) :
    Button(x, y, 20, 22, Component.empty(), onPress, DEFAULT_NARRATION) {
    private var isSelected = false

    override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        pGuiGraphics.pose().pushPose()
        RenderSystem.enableDepthTest()

        if (this.isSelected) {
            pGuiGraphics.blit(
                VehicleAssemblingScreen.TEXTURE,
                this.x,
                this.y,
                179f,
                182f,
                23,
                this.height,
                VehicleAssemblingScreen.IMAGE_SIZE,
                VehicleAssemblingScreen.IMAGE_SIZE
            )
        } else {
            pGuiGraphics.blit(
                VehicleAssemblingScreen.TEXTURE,
                this.x,
                this.y,
                179f,
                205f,
                20,
                this.height,
                VehicleAssemblingScreen.IMAGE_SIZE,
                VehicleAssemblingScreen.IMAGE_SIZE
            )
        }

        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )

        val vOffset = when (this.category) {
            VehicleAssemblingRecipe.Category.LAND -> 182
            VehicleAssemblingRecipe.Category.DEFENSE -> 198
            VehicleAssemblingRecipe.Category.AIRCRAFT -> 214
            VehicleAssemblingRecipe.Category.WATER -> 230
            VehicleAssemblingRecipe.Category.CIVILIAN -> 246
            else -> 262
        }
        pGuiGraphics.blit(
            VehicleAssemblingScreen.TEXTURE,
            this.x + 3,
            this.y + 3,
            (if (this.isSelected) 203 else 221).toFloat(),
            vOffset.toFloat(),
            16,
            16,
            VehicleAssemblingScreen.IMAGE_SIZE,
            VehicleAssemblingScreen.IMAGE_SIZE
        )

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        pGuiGraphics.pose().popPose()
    }

    override fun onPress() {
        this.isSelected = true
        this.onPress.onPress(this)
    }

    fun setSelected(selected: Boolean) {
        this.isSelected = selected
    }

    fun renderTooltips(pGuiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (this.isHovered()) {
            pGuiGraphics.renderTooltip(
                mc.font,
                Component.translatable("tips.superbwarfare.category." + this.category.typeName),
                mouseX,
                mouseY
            )
        }
    }
}
