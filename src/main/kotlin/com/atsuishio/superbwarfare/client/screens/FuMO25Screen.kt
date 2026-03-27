package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.network.message.send.RadarChangeModeMessage
import com.atsuishio.superbwarfare.network.message.send.RadarSetParametersMessage
import com.atsuishio.superbwarfare.network.message.send.RadarSetPosMessage
import com.atsuishio.superbwarfare.network.message.send.RadarSetTargetMessage
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*

@OnlyIn(Dist.CLIENT)
class FuMO25Screen(pMenu: FuMO25Menu, pPlayerInventory: Inventory, pTitle: Component) :
    AbstractContainerScreen<FuMO25Menu>(pMenu, pPlayerInventory, pTitle) {
    private var currentPos: BlockPos? = null
    private var currentTarget: Entity? = null

    init {
        imageWidth = 340
        imageHeight = 166
    }

    override fun renderBg(pGuiGraphics: GuiGraphics, pPartialTick: Float, pMouseX: Int, pMouseY: Int) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2
        pGuiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 358, 328)

        // 目标位置
        renderTargets(pGuiGraphics)

        // 扫描盘
        renderScan(pGuiGraphics)

        // 网格线
        renderXLine(pGuiGraphics, i, j)

        // FE
        val energy = this.menu!!.energy
        val energyRate = energy.toFloat() / FuMO25BlockEntity.MAX_ENERGY.toFloat()
        pGuiGraphics.blit(TEXTURE, i + 278, j + 39, 178f, 167f, (54 * energyRate).toInt(), 16, 358, 328)

        // 信息显示
        renderInfo(pGuiGraphics)

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    private fun renderXLine(guiGraphics: GuiGraphics, i: Int, j: Int) {
        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE)

        guiGraphics.blit(TEXTURE, i + 8, j + 11, 0f, 167f, 147, 147, 358, 328)

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )

        poseStack.popPose()
    }

    private fun renderTargets(guiGraphics: GuiGraphics) {
        val entities = FuMO25ScreenHelper.entities
        if (entities.isNullOrEmpty()) return
        val pos = FuMO25ScreenHelper.pos ?: return
        if (!this.menu!!.isPowered) return

        val type = this.menu!!.funcType.toInt()
        val range = if (type == 1) FuMO25BlockEntity.MAX_RANGE else FuMO25BlockEntity.DEFAULT_RANGE

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val centerX = i + 81
        val centerY = j + 84

        for (entity in entities) {
            val moveX = (entity.x - pos.x) / range * 74
            val moveZ = (entity.z - pos.z) / range * 74

            RenderHelper.preciseBlit(
                guiGraphics, TEXTURE, (centerX + moveX).toFloat(), (centerY + moveZ).toFloat(),
                233f, 167f, 4f, 4f, 358f, 328f
            )
        }

        poseStack.popPose()
    }

    private fun renderScan(guiGraphics: GuiGraphics) {
        if (this.menu!!.energy <= 0) return
        if (!this.menu!!.isPowered) return

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(System.currentTimeMillis() % 36000000 / 30f),
            i + 9 + 145 / 2f,
            j + 12 + 145 / 2f,
            0f
        )

        guiGraphics.blit(SCAN, i + 9, j + 12, 0f, 0f, 145, 145, 145, 145)

        poseStack.popPose()
    }

    private fun renderInfo(guiGraphics: GuiGraphics) {
        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        if (this.currentPos != null) {
            guiGraphics.drawString(
                this.font, Component.translatable(
                    "des.superbwarfare.fumo_25.current_pos",
                    "[${currentPos!!.x}, ${currentPos!!.y}, ${currentPos!!.z}]"
                ), i + 173, j + 13, 0xffffff
            )
        }

        if (this.currentTarget != null) {
            val sb = StringBuilder()
            sb.append(currentTarget!!.displayName.string)
            val ct = currentTarget!!
            if (ct is LivingEntity) {
                sb.append(" (HP: ${format1D(ct.health.toDouble())}/${format1D(ct.maxHealth.toDouble())})")
            } else if (ct is VehicleEntity) {
                sb.append(" (HP: ${format1D(ct.health.toDouble())}/${format1D(ct.getMaxHealth().toDouble())})")
            }

            guiGraphics.drawString(
                this.font, Component.translatable("des.superbwarfare.fumo_25.current_target", sb),
                i + 173, j + 24, 0xffffff
            )
        }

        val type = this.menu!!.funcType.toInt()
        val component = when (type) {
            1 -> Component.translatable("des.superbwarfare.fumo_25.type_1")
            2 -> Component.translatable("des.superbwarfare.fumo_25.type_2")
            3 -> Component.translatable("des.superbwarfare.fumo_25.type_3")
            else -> Component.translatable("des.superbwarfare.fumo_25.type_0")
        }
        guiGraphics.drawString(this.font, component, i + 173, j + 43, 0xffffff)
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val entities = FuMO25ScreenHelper.entities
        if (entities.isNullOrEmpty()) return super.mouseClicked(pMouseX, pMouseY, pButton)
        val pos = FuMO25ScreenHelper.pos ?: return super.mouseClicked(pMouseX, pMouseY, pButton)
        if (pButton != 0) return super.mouseClicked(pMouseX, pMouseY, pButton)
        if (!this@FuMO25Screen.menu!!.isPowered) return super.mouseClicked(pMouseX, pMouseY, pButton)

        val type = this@FuMO25Screen.menu!!.funcType.toInt()
        val range = if (type == 1) FuMO25BlockEntity.MAX_RANGE else FuMO25BlockEntity.DEFAULT_RANGE

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val centerX = i + 81
        val centerY = j + 84

        for (entity in entities) {
            val moveX = (entity.x - pos.x) / range * 74
            val moveZ = (entity.z - pos.z) / range * 74

            if (pMouseX in centerX + moveX..centerX + moveX + 4 && pMouseY in centerY + moveZ..centerY + moveZ + 4) {
                sendPacketToServer(RadarSetPosMessage(entity.onPos))
                this.currentPos = entity.onPos
                this.currentTarget = entity
                return true
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        pGuiGraphics.pose().pushPose()
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        pGuiGraphics.pose().popPose()
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY)
    }

    override fun renderTooltip(pGuiGraphics: GuiGraphics, pX: Int, pY: Int) {
        super.renderTooltip(pGuiGraphics, pX, pY)

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val tooltip: MutableList<Component?> = ArrayList<Component?>()
        tooltip.add(
            Component.translatable(
                "des.superbwarfare.charging_station.energy", this.menu!!.energy,
                FuMO25BlockEntity.MAX_ENERGY
            )
        )

        if (pX - i in 278..332 && pY - j in 39..55) {
            pGuiGraphics.renderTooltip(this.font, tooltip, Optional.empty(), pX, pY)
        }
    }

    // 本方法留空
    override fun renderLabels(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int) {
    }

    override fun init() {
        super.init()
        this.titleLabelX = 33
        this.titleLabelY = 5
        this.inventoryLabelX = 105
        this.inventoryLabelY = 128

        this.currentPos = null
        this.currentTarget = null

        val i = (this.width - this.imageWidth) / 2
        val j = (this.height - this.imageHeight) / 2

        val lockButton = LockButton(i + 304, j + 61)
        this.addRenderableWidget(lockButton)

        val widerButton = ModeButton(i + 171, j + 61, 1)
        this.addRenderableWidget(widerButton)

        val glowButton = ModeButton(i + 201, j + 61, 2)
        this.addRenderableWidget(glowButton)

        val guideButton = ModeButton(i + 231, j + 61, 3)
        this.addRenderableWidget(guideButton)
    }

    @OnlyIn(Dist.CLIENT)
    internal inner class LockButton(pX: Int, pY: Int) : AbstractButton(pX, pY, 29, 15, Component.empty()) {
        override fun onPress() {
            if (this@FuMO25Screen.menu!!.funcType == 3L && this@FuMO25Screen.menu!!.getSlot(0).item.isEmpty) {
                if (this@FuMO25Screen.currentTarget == null) return
                sendPacketToServer(RadarSetTargetMessage(this@FuMO25Screen.currentTarget!!.getUUID()))
            } else {
                sendPacketToServer(RadarSetParametersMessage)
            }
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            if (this@FuMO25Screen.menu!!.funcType == 3L && this@FuMO25Screen.menu!!.getSlot(0).item.isEmpty) {
                pGuiGraphics.blit(
                    TEXTURE,
                    this.x,
                    this.y,
                    148f,
                    (if (this.isHovered()) 311 else 295).toFloat(),
                    29,
                    15,
                    358,
                    328
                )
            } else {
                pGuiGraphics.blit(
                    TEXTURE,
                    this.x,
                    this.y,
                    148f,
                    (if (this.isHovered()) 183 else 167).toFloat(),
                    29,
                    15,
                    358,
                    328
                )
            }
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    @OnlyIn(Dist.CLIENT)
    internal class ModeButton(pX: Int, pY: Int, private val mode: Int) :
        AbstractButton(pX, pY, 29, 15, Component.empty()) {
        override fun onPress() {
            sendPacketToServer(RadarChangeModeMessage(this.mode.toByte()))
        }

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            pGuiGraphics.blit(
                TEXTURE,
                this.x,
                this.y,
                148f,
                (if (this.isHovered()) 183 + this.mode * 32 else 167 + this.mode * 32).toFloat(),
                29,
                15,
                358,
                328
            )
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }

    companion object {
        private val TEXTURE = loc("textures/gui/radar.png")
        private val SCAN = loc("textures/gui/radar_scan.png")
    }
}
