package com.atsuishio.superbwarfare.client.map.context

import com.atsuishio.superbwarfare.client.map.context.MapMarker.Companion.getColorRGB
import com.atsuishio.superbwarfare.client.map.context.MapMarker.Companion.rgbToFloat3
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.roundToInt

@OnlyIn(Dist.CLIENT)
class MapContextMenu {

    // ── Context menu state ──
    var ctxMenuVisible = false
        private set
    var ctxMenuX = 0
        private set
    var ctxMenuY = 0
        private set
    var ctxWorldX = 0
        private set
    var ctxWorldY = 0
        private set
    var ctxWorldZ = 0
        private set
    var ctxTargetMarker: MapMarker? = null
        private set

    // ── Edit panel state ──
    var editPanelVisible = false
        private set
    var editMarker: MapMarker? = null
        private set
    var editColorIndex = 0
        private set

    private var editNameBox: EditBox? = null
    private var editPanelX = 0
    private var editPanelY = 0

    // Callback: called when a marker is created or edited
    var onMarkerCreated: ((MapMarker) -> Unit)? = null
    var onMarkerEdited: ((MapMarker) -> Unit)? = null
    // Callback: delete a marker
    var onMarkerDelete: ((MapMarker) -> Unit)? = null
    var onConnectRequested: ((MapMarker) -> Unit)? = null

    // ── Internal data class for menu items ──
    data class ContextMenuItem(val label: String, val action: () -> Unit)

    // ── Public API: open menus ──

    fun openMapMenu(screenX: Int, screenY: Int, worldX: Int, worldY: Int, worldZ: Int) {
        ctxTargetMarker = null
        ctxWorldX = worldX
        ctxWorldY = worldY
        ctxWorldZ = worldZ
        ctxMenuX = screenX
        ctxMenuY = screenY
        ctxMenuVisible = true
        editPanelVisible = false
    }

    fun openMarkerMenu(screenX: Int, screenY: Int, marker: MapMarker) {
        ctxTargetMarker = marker
        ctxWorldX = marker.x
        ctxWorldY = marker.y
        ctxWorldZ = marker.z
        ctxMenuX = screenX
        ctxMenuY = screenY
        ctxMenuVisible = true
        editPanelVisible = false
    }

    fun closeMenu() {
        ctxMenuVisible = false
        ctxTargetMarker = null
    }

    fun openEditPanel(marker: MapMarker?) {
        editMarker = marker
        editColorIndex = marker?.colorIndex ?: 15 // default white
        editPanelVisible = true
        ctxMenuVisible = false
        closeEditBox()
        editNameBox = null // will be re-created on next render
    }

    fun closeEditPanel() {
        editPanelVisible = false
        editMarker = null
        closeEditBox()
    }

    private fun closeEditBox() {
        editNameBox?.isFocused = false
        editNameBox = null
    }

    val isEditing: Boolean get() = editPanelVisible && editNameBox != null

    // ── Menu items builder ──

    private fun buildItems(): List<ContextMenuItem> {
        val target = ctxTargetMarker
        return if (target != null) {
            listOf(
                ContextMenuItem(
                    Component.translatable(
                        "context.superbwarfare.tactical_map.teleport",
                        target.x, target.y + 1, target.z
                    ).string
                ) {
                    val mc = Minecraft.getInstance()
                    mc.player?.connection?.sendCommand("tp ${target.x} ${target.y + 1} ${target.z}")
                },
                ContextMenuItem(
                    Component.translatable("context.superbwarfare.tactical_map.edit_marker").string
                ) {
                    openEditPanel(target)
                },
                ContextMenuItem(
                    Component.translatable("context.superbwarfare.tactical_map.connect").string
                ) {
                    onConnectRequested?.invoke(target)
                },
                ContextMenuItem(
                    Component.translatable("context.superbwarfare.tactical_map.delete_marker").string
                ) {
                    onMarkerDelete?.invoke(target)
                }
            )
        } else {
            listOf(
                ContextMenuItem(
                    Component.translatable(
                        "context.superbwarfare.tactical_map.teleport",
                        ctxWorldX, ctxWorldY + 1, ctxWorldZ
                    ).string
                ) {
                    val mc = Minecraft.getInstance()
                    mc.player?.connection?.sendCommand("tp $ctxWorldX ${ctxWorldY + 1} $ctxWorldZ")
                },
                ContextMenuItem(
                    Component.translatable("context.superbwarfare.tactical_map.create_marker").string
                ) {
                    openEditPanel(null)
                }
            )
        }
    }

    // ── Marker hit-test ──

    /**
     * 检测鼠标是否点击了某个标记点（判定区域 = 整张贴图矩形，底边中点锚定）。
     *
     * @return 命中的 [MapMarker]，未命中则返回 null
     */
    fun hitTestMarker(
        markers: List<MapMarker>,
        mouseX: Double,
        mouseY: Double,
        viewBlockX: Double,
        viewBlockZ: Double,
        scale: Double,
        mapCenterX: Float,
        mapCenterY: Float
    ): MapMarker? {
        val hw = MapContextMenu.MARKER_TEX_W / 2.0
        val hh = MapContextMenu.MARKER_TEX_H.toDouble()
        for (marker in markers) {
            // 锚点（底边中点）的屏幕坐标
            val ax = mapCenterX + (marker.x - viewBlockX) * scale
            val ay = mapCenterY + (marker.z - viewBlockZ) * scale
            // 矩形：左 = ax - hw, 右 = ax + hw, 上 = ay - hh, 下 = ay
            if (mouseX >= ax - hw && mouseX <= ax + hw && mouseY >= ay - hh && mouseY <= ay) {
                return marker
            }
        }
        return null
    }

    // ── Rendering ──

    fun render(
        guiGraphics: GuiGraphics,
        font: net.minecraft.client.gui.Font,
        mouseX: Int,
        mouseY: Int,
        screenWidth: Int,
        screenHeight: Int
    ) {
        if (ctxMenuVisible) {
            renderContextMenu(guiGraphics, font, mouseX, mouseY, screenWidth, screenHeight)
        }
        if (editPanelVisible) {
            renderEditPanel(guiGraphics, font, mouseX, mouseY, screenWidth, screenHeight)
        }
    }

    // ── Context menu rendering ──

    private fun renderContextMenu(
        guiGraphics: GuiGraphics,
        font: net.minecraft.client.gui.Font,
        mouseX: Int,
        mouseY: Int,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val items = buildItems()
        if (items.isEmpty()) return

        val padding = 4
        val itemHeight = 12
        val menuW = items.maxOf { font.width(it.label) } + padding * 2
        val menuH = items.size * itemHeight + padding * 2 + 2

        var mx = ctxMenuX + 8
        var my = ctxMenuY
        if (mx + menuW > screenWidth) mx = ctxMenuX - menuW - 8
        if (my + menuH > screenHeight) my = screenHeight - menuH - 4

        // Background
        guiGraphics.fill(mx, my, mx + menuW, my + menuH, 0xEE2A2A2A.toInt())
        guiGraphics.fill(mx, my, mx + menuW, my + 1, 0xFF555555.toInt())
        guiGraphics.fill(mx, my + menuH - 1, mx + menuW, my + menuH, 0xFF555555.toInt())
        guiGraphics.fill(mx, my, mx + 1, my + menuH, 0xFF555555.toInt())
        guiGraphics.fill(mx + menuW - 1, my, mx + menuW, my + menuH, 0xFF555555.toInt())

        // Items
        for ((i, item) in items.withIndex()) {
            val iy = my + padding + i * itemHeight
            val hovered = mouseX in mx..mx + menuW && mouseY in iy..iy + itemHeight
            if (hovered) guiGraphics.fill(mx + 1, iy, mx + menuW - 1, iy + itemHeight, 0x664444FF)
            guiGraphics.drawString(
                font, item.label, mx + padding, iy + 2,
                if (hovered) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt(), false
            )
        }
    }

    // ── Context menu click handling ──

    fun handleContextMenuClick(mouseX: Double, mouseY: Double, font: net.minecraft.client.gui.Font, screenWidth: Int, screenHeight: Int): Boolean {
        if (!ctxMenuVisible) return false

        val items = buildItems()
        if (items.isEmpty()) return false

        val padding = 4
        val itemHeight = 12
        val menuW = items.maxOf { font.width(it.label) } + padding * 2
        val menuH = items.size * itemHeight + padding * 2 + 2
        var mx = ctxMenuX + 8
        var my = ctxMenuY
        if (mx + menuW > screenWidth) mx = ctxMenuX - menuW - 8
        if (my + menuH > screenHeight) my = screenHeight - menuH - 4

        for ((i, item) in items.withIndex()) {
            val iy = my + padding + i * itemHeight
            if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in iy.toDouble()..(iy + itemHeight).toDouble()) {
                item.action()
                ctxMenuVisible = false
                return true
            }
        }
        ctxMenuVisible = false
        return true
    }

    // ── Edit panel rendering ──

    private fun renderEditPanel(
        guiGraphics: GuiGraphics,
        font: net.minecraft.client.gui.Font,
        mouseX: Int,
        mouseY: Int,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val isCreating = editMarker == null
        val panelW = 180
        val panelH = 170
        val px = (screenWidth - panelW) / 2
        val py = (screenHeight - panelH) / 2
        editPanelX = px
        editPanelY = py

        // Semi-transparent background
        guiGraphics.fill(px, py, px + panelW, py + panelH, 0xEE1E1E1E.toInt())
        guiGraphics.fill(px - 1, py - 1, px + panelW + 1, py, 0xFF555555.toInt())
        guiGraphics.fill(px - 1, py + panelH, px + panelW + 1, py + panelH + 1, 0xFF555555.toInt())
        guiGraphics.fill(px - 1, py, px, py + panelH, 0xFF555555.toInt())
        guiGraphics.fill(px + panelW, py, px + panelW + 1, py + panelH, 0xFF555555.toInt())

        // Title
        val titleKey = if (isCreating) "context.superbwarfare.tactical_map.create_marker_title"
        else "context.superbwarfare.tactical_map.edit_marker_title"
        val title = Component.translatable(titleKey).string
        guiGraphics.drawString(font, title, px + (panelW - font.width(title)) / 2, py + 6, 0xFFFFFFFF.toInt(), false)

        // Name label + EditBox
        val label = Component.translatable("context.superbwarfare.tactical_map.marker_name").string
        guiGraphics.drawString(font, label, px + 8, py + 26, 0xFFCCCCCC.toInt(), false)

        // Lazy-create EditBox
        val box: EditBox
        if (editNameBox == null) {
            box = EditBox(font, px + 8, py + 40, panelW - 16, 16, Component.literal(""))
            box.setMaxLength(32)
            box.isFocused = true
            if (editMarker != null) {
                box.value = editMarker!!.name
            }
            editNameBox = box
        } else {
            box = editNameBox!!
        }

        box.x = px + 8
        box.y = py + 40
        box.render(guiGraphics, mouseX, mouseY, 0f)

        // Color picker (4x4 grid)
        val colorGridX = px + 8
        val colorGridY = py + 62
        val cellSize = 16
        val cellGap = 2
        for (i in 0..15) {
            val cx = colorGridX + (i % 4) * (cellSize + cellGap)
            val cy = colorGridY + (i / 4) * (cellSize + cellGap)
            val color = getColorRGB(i)
            // Fill with color
            guiGraphics.fill(cx, cy, cx + cellSize, cy + cellSize, 0xFF000000.toInt() or color)
            // Selection highlight
            if (i == editColorIndex) {
                guiGraphics.fill(cx - 1, cy - 1, cx + cellSize + 1, cy, 0xFFFFFFFF.toInt())
                guiGraphics.fill(cx - 1, cy + cellSize, cx + cellSize + 1, cy + cellSize + 1, 0xFFFFFFFF.toInt())
                guiGraphics.fill(cx - 1, cy, cx, cy + cellSize, 0xFFFFFFFF.toInt())
                guiGraphics.fill(cx + cellSize, cy, cx + cellSize + 1, cy + cellSize, 0xFFFFFFFF.toInt())
            }
            // Hover highlight
            if (mouseX in cx..cx + cellSize && mouseY in cy..cy + cellSize) {
                guiGraphics.fill(cx, cy, cx + cellSize, cy + cellSize, 0x44FFFFFF)
            }
        }

        // OK button
        val okLabel = Component.translatable("context.superbwarfare.tactical_map.ok").string
        val btnW = 60
        val btnH = 16
        val okX = px + panelW - btnW - 8 - btnW - 6
        val okY = py + panelH - btnH - 10
        val okHovered = mouseX in okX..okX + btnW && mouseY in okY..okY + btnH
        guiGraphics.fill(okX, okY, okX + btnW, okY + btnH, if (okHovered) 0xFF446644.toInt() else 0xFF335533.toInt())
        guiGraphics.drawString(font, okLabel, okX + (btnW - font.width(okLabel)) / 2, okY + 4, 0xFFFFFFFF.toInt(), false)

        // Cancel button
        val cancelLabel = Component.translatable("context.superbwarfare.tactical_map.cancel").string
        val cancelX = px + panelW - btnW - 8
        val cancelY = okY
        val cancelHovered = mouseX in cancelX..cancelX + btnW && mouseY in cancelY..cancelY + btnH
        guiGraphics.fill(cancelX, cancelY, cancelX + btnW, cancelY + btnH, if (cancelHovered) 0xFF664444.toInt() else 0xFF553333.toInt())
        guiGraphics.drawString(font, cancelLabel, cancelX + (btnW - font.width(cancelLabel)) / 2, cancelY + 4, 0xFFFFFFFF.toInt(), false)
    }

    // ── Edit panel click handling ──

    fun handleEditPanelClick(mouseX: Double, mouseY: Double): Boolean {
        if (!editPanelVisible) return false

        val px = editPanelX
        val py = editPanelY
        val panelW = 180
        val panelH = 170

        // If click is outside the panel, ignore
        if (mouseX < px || mouseX > px + panelW || mouseY < py || mouseY > py + panelH) {
            return false
        }

        // Color grid
        val colorGridX = px + 8
        val colorGridY = py + 62
        val cellSize = 16
        val cellGap = 2
        for (i in 0..15) {
            val cx = colorGridX + (i % 4) * (cellSize + cellGap)
            val cy = colorGridY + (i / 4) * (cellSize + cellGap)
            if (mouseX in cx.toDouble()..(cx + cellSize).toDouble() &&
                mouseY in cy.toDouble()..(cy + cellSize).toDouble()
            ) {
                editColorIndex = i
                return true
            }
        }

        // OK button
        val btnW = 60
        val btnH = 16
        val okX = px + panelW - btnW - 8 - btnW - 6
        val okY = py + panelH - btnH - 10
        if (mouseX in okX.toDouble()..(okX + btnW).toDouble() &&
            mouseY in okY.toDouble()..(okY + btnH).toDouble()
        ) {
            confirmEdit()
            return true
        }

        // Cancel button
        val cancelX = px + panelW - btnW - 8
        val cancelY = okY
        if (mouseX in cancelX.toDouble()..(cancelX + btnW).toDouble() &&
            mouseY in cancelY.toDouble()..(cancelY + btnH).toDouble()
        ) {
            closeEditPanel()
            return true
        }

        return true // click inside panel consumes the event
    }

    private fun confirmEdit() {
        val name = editNameBox?.value?.trim() ?: ""
        if (name.isEmpty()) return

        val existing = editMarker
        if (existing != null) {
            // Edit existing marker
            existing.name = name
            existing.colorIndex = editColorIndex
            onMarkerEdited?.invoke(existing)
        } else {
            // Create new marker
            val newMarker = MapMarker(
                name = name,
                x = ctxWorldX,
                y = ctxWorldY,
                z = ctxWorldZ,
                colorIndex = editColorIndex
            )
            onMarkerCreated?.invoke(newMarker)
        }
        closeEditPanel()
    }

    // ── EditBox event delegation (called from TacticalMapScreen) ──

    fun editBoxMouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return editNameBox?.mouseClicked(mouseX, mouseY, button) ?: false
    }

    fun editBoxKeyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return editNameBox?.keyPressed(keyCode, scanCode, modifiers) ?: false
    }

    fun editBoxCharTyped(codePoint: Char, modifiers: Int): Boolean {
        // Enter key confirms
        if (codePoint == '\r' || codePoint == '\n') {
            confirmEdit()
            return true
        }
        return editNameBox?.charTyped(codePoint, modifiers) ?: false
    }

    fun editBoxTick() {
        editNameBox?.tick()
    }

    // ── Marker rendering ──

    companion object {
        /**
         * 标记点贴图的显示尺寸，必须与实际贴图文件的像素尺寸一致。
         * 如需调整贴图大小，修改贴图文件后同步更新此处的值。
         */
        const val MARKER_TEX_W = 8
        const val MARKER_TEX_H = 13

        /**
         * 渲染单个标记点（由 TacticalMapScreen 调用）。
         *
         * 贴图底边中点锚定于标记点的世界坐标位置，
         * 整张贴图区域作为交互判定区域。
         */
        fun renderMarker(
            guiGraphics: GuiGraphics,
            font: net.minecraft.client.gui.Font,
            marker: MapMarker,
            viewBlockX: Double,
            viewBlockZ: Double,
            scale: Double,
            mapCenterX: Float,
            mapCenterY: Float,
            mapLeft: Int,
            mapTop: Int,
            mapAreaW: Int,
            mapAreaH: Int,
            markerTexture: net.minecraft.resources.ResourceLocation,
            isDragging: Boolean
        ) {
            // 世界坐标 → 屏幕坐标（贴图底边中点 = 世界坐标锚点）
            val anchorX = mapCenterX + (marker.x - viewBlockX) * scale
            val anchorY = mapCenterY + (marker.z - viewBlockZ) * scale

            // 将锚点 clamp 到地图区域内，确保整张贴图可见
            val clampedAX = anchorX.coerceIn(
                (mapLeft + MARKER_TEX_W / 2).toDouble(),
                (mapLeft + mapAreaW - MARKER_TEX_W / 2).toDouble()
            )
            val clampedAY = anchorY.coerceIn(
                (mapTop + MARKER_TEX_H).toDouble(),
                (mapTop + mapAreaH).toDouble()
            )
            val alpha = if (clampedAX == anchorX && clampedAY == anchorY) 1f else 0.5f

            val (r, g, b) = rgbToFloat3(getColorRGB(marker.colorIndex))

            // 绘制贴图：底边中点对准锚点
            val pose = guiGraphics.pose()
            pose.pushPose()
            pose.translate(clampedAX.toFloat(), clampedAY.toFloat(), 0f)

            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, alpha)
            com.mojang.blaze3d.systems.RenderSystem.enableBlend()
            guiGraphics.blit(
                markerTexture,
                -MARKER_TEX_W / 2, -MARKER_TEX_H,
                MARKER_TEX_W, MARKER_TEX_H,
                0f, 0f, MARKER_TEX_W, MARKER_TEX_H, MARKER_TEX_W, MARKER_TEX_H
            )
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            pose.popPose()

            // 标记点名称 — 仅在未被 clamp 到边缘时显示
            if (alpha >= 1f) {
                val textWidth = font.width(marker.name)
                val textX = clampedAX.toFloat() - textWidth / 2f
                // 字符底部高于贴图顶部 1 像素
                val textY = clampedAY.toFloat() - MARKER_TEX_H - font.lineHeight - 1f
                val textColor = if (isDragging) 0xFFFFFF55.toInt() else 0xFFFFFFFF.toInt()
                guiGraphics.drawString(font, marker.name, textX.roundToInt() + 1, textY.roundToInt() + 1, 0x66000000, false)
                guiGraphics.drawString(font, marker.name, textX.roundToInt(), textY.roundToInt(), textColor, false)
            }
        }
    }
}
