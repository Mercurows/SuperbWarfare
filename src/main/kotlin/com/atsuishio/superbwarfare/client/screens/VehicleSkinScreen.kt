package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.data.vehicle_skin.SkinInfo
import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkin
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.network.message.send.SetVehicleSkinMessage
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import com.mojang.blaze3d.platform.Lighting
import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractButton
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class VehicleSkinScreen(private val entity: Entity) : Screen(Component.empty()) {

    private val columns = 4
    private val buttonWidth = 110
    private val buttonHeight = 160
    private val gapX = 10
    private val gapY = 10

    private val previewEntities = mutableMapOf<String, VehicleEntity>()

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun init() {
        super.init()
        this.clearWidgets()
        previewEntities.clear()

        val vehicle = entity as? VehicleEntity ?: return
        val currentSkinId = vehicle.skinId
        val vehicleType = vehicle.type

        // Build skin list: vanilla always first
        val skinEntries = mutableListOf<Pair<String, SkinInfo?>>()
        skinEntries.add("" to null)

        val skinData = VehicleSkin.getSkins(vehicleType)
        skinEntries.addAll(skinData.skins
            .filter { it.id != "vanilla" }
            .map { it.id to it }
        )

        // Create client-side-only preview entities for each skin
        val clientLevel = mc.level ?: return
        for ((skinId, _) in skinEntries) {
            val previewEntity = vehicleType.create(clientLevel) as? VehicleEntity ?: continue
            previewEntity.skinId = skinId
            previewEntities[skinId] = previewEntity
        }

        // Calculate grid position
        val rows = (skinEntries.size + columns - 1) / columns
        val totalWidth = columns * buttonWidth + (columns - 1) * gapX
        val startX = (this.width - totalWidth) / 2
        val startY = (this.height - (rows * buttonHeight + (rows - 1) * gapY)) / 2

        for ((index, entry) in skinEntries.withIndex()) {
            val (skinId, skinInfo) = entry
            val col = index % columns
            val row = index / columns
            val x = startX + col * (buttonWidth + gapX)
            val y = startY + row * (buttonHeight + gapY)

            val isSelected = if (skinId.isBlank()) {
                currentSkinId.isBlank()
            } else {
                currentSkinId == skinId
            }

            this.addRenderableWidget(
                SkinSlotButton(
                    x, y, buttonWidth, buttonHeight,
                    skinId, skinInfo,
                    previewEntities[skinId],
                    entity.id,
                    isSelected
                )
            )
        }
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        this.renderBackground(pGuiGraphics)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val hitButton = this.children().any {
            it is AbstractButton && it.isMouseOver(pMouseX, pMouseY)
        }
        if (!hitButton) {
            this.onClose()
            return true
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun removed() {
        super.removed()
        previewEntities.values.forEach { it.discard() }
        previewEntities.clear()
    }

    @OnlyIn(Dist.CLIENT)
    private inner class SkinSlotButton(
        pX: Int, pY: Int, pWidth: Int, pHeight: Int,
        private val skinId: String,
        private val skinInfo: SkinInfo?,
        private val previewEntity: VehicleEntity?,
        private val vehicleEntityId: Int,
        private val isSelected: Boolean
    ) : AbstractButton(pX, pY, pWidth, pHeight, Component.empty()) {

        override fun renderWidget(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
            val pose = pGuiGraphics.pose()

            // Draw selection highlight
            if (isSelected) {
                pGuiGraphics.fill(this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, -0x2900)
            }

            // Draw hover highlight
            if (this.isHovered && this.isActive) {
                pGuiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x33FFFFFF)
            }

            // Render 3D vehicle preview
            if (previewEntity != null) {
                pose.pushPose()

                // Position: center of slot, upper portion for 3D model
                val centerX = this.x + this.width / 2.0
                val centerY = this.y + 56.0
                pose.translate(centerX, centerY, 80.0)

                // Scale to fit slot — normalize by bounding box size
                val bbSize = previewEntity.boundingBox.size.toFloat()
                val scale = (this.width * 0.35f) / maxOf(bbSize, 1f)
                pose.scale(scale, scale, -scale)

                // Flip right-side up (vanilla InventoryScreen uses rotateZ(PI))
                pose.mulPose(Axis.ZP.rotationDegrees(180f))
                // Nice 3/4 angle view
                pose.mulPose(Axis.YP.rotationDegrees(150f))
                pose.mulPose(Axis.XP.rotationDegrees(15f))

                Lighting.setupForEntityInInventory()
                val erd = mc.entityRenderDispatcher
                erd.setRenderShadow(false)
                erd.render(
                    previewEntity,
                    0.0, 0.0, 0.0,
                    0f, 1f,
                    pose,
                    pGuiGraphics.bufferSource(),
                    15728880
                )
                pGuiGraphics.flush()
                erd.setRenderShadow(true)
                Lighting.setupFor3DItems()

                pose.popPose()
            }

            // Draw skin id, name, description below the preview
            val textY = this.y + this.height - 36
            val displayName = skinInfo?.name ?: "Vanilla"
            val displayId = skinId.ifBlank { "vanilla" }
            val description = skinInfo?.description ?: ""

            val font = this@VehicleSkinScreen.font
            val textColor = if (isSelected) 0xFFD700 else 0xFFFFFF

            val nameText = font.plainSubstrByWidth(displayName, this.width - 4)
            val idText = font.plainSubstrByWidth(displayId, this.width - 4)

            pGuiGraphics.drawCenteredString(font, nameText, this.x + this.width / 2, textY, textColor)
            pGuiGraphics.drawCenteredString(font, idText, this.x + this.width / 2, textY + 10, 0xAAAAAA)

            if (description.isNotBlank()) {
                val descText = font.plainSubstrByWidth(description, this.width - 4)
                pGuiGraphics.drawCenteredString(font, descText, this.x + this.width / 2, textY + 20, 0x888888)
            }
        }

        override fun onPress() {
            if (!this.isActive) return
            sendPacketToServer(SetVehicleSkinMessage(vehicleEntityId, skinId))
            this@VehicleSkinScreen.onClose()
        }

        override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        }
    }
}
