package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen.Companion.SelBox
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import kotlin.math.abs

/**
 * 战术地图区域选择框管理工具。
 * 处理选择框渲染、命中测试、右键菜单和鼠标释放时的框创建。
 */
object SelectionBoxManager {

    data class Rect4f(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

    // ── Rendering ──

    fun render(
        guiGraphics: GuiGraphics,
        selBoxes: List<SelBox>,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, zoom: Double,
        selectionDragging: Boolean,
        selDragStartX: Float, selDragStartY: Float,
        selDragEndX: Float, selDragEndY: Float,
        font: Font,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int,
    ) {
        val scale = CoordinateConverter.scaleFromZoom(zoom)

        // All confirmed boxes
        for (box in selBoxes) {
            val minSX = CoordinateConverter.worldToScreenX(box.worldMinX, mapCenterX, viewBlockX, scale).toFloat()
            val maxSX = CoordinateConverter.worldToScreenX(box.worldMaxX, mapCenterX, viewBlockX, scale).toFloat()
            val minSY = CoordinateConverter.worldToScreenY(box.worldMinZ, mapCenterY, viewBlockZ, scale).toFloat()
            val maxSY = CoordinateConverter.worldToScreenY(box.worldMaxZ, mapCenterY, viewBlockZ, scale).toFloat()
            guiGraphics.fill(minSX.toInt(), minSY.toInt(), maxSX.toInt(), maxSY.toInt(), 0x220000AA)
            val bc = 0xCCFFFFFF.toInt()
            guiGraphics.fill(minSX.toInt(), minSY.toInt(), maxSX.toInt(), minSY.toInt() + 1, bc)
            guiGraphics.fill(minSX.toInt(), maxSY.toInt(), maxSX.toInt(), maxSY.toInt() + 1, bc)
            guiGraphics.fill(minSX.toInt(), minSY.toInt(), minSX.toInt() + 1, maxSY.toInt(), bc)
            guiGraphics.fill(maxSX.toInt(), minSY.toInt(), maxSX.toInt() + 1, maxSY.toInt(), bc)
            val wW = abs(box.worldMaxX - box.worldMinX).toInt()
            val wH = abs(box.worldMaxZ - box.worldMinZ).toInt()
            val labelW = "宽: ${wW}m"
            val labelH = "长: ${wH}m"
            val lw = font.width(labelW)
            guiGraphics.drawString(
                font,
                labelW,
                ((minSX + maxSX) / 2 - lw / 2).toInt(),
                minSY.toInt() - 10,
                0xFFFFFFFF.toInt(),
                true
            )
            guiGraphics.drawString(
                font,
                labelH,
                maxSX.toInt() + 3,
                ((minSY + maxSY) / 2 - font.lineHeight / 2).toInt(),
                0xFFFFFFFF.toInt(),
                true
            )
        }

        // Dashed preview during drag
        if (selectionDragging) {
            val minSX = minOf(selDragStartX, selDragEndX)
            val maxSX = maxOf(selDragStartX, selDragEndX)
            val minSY = minOf(selDragStartY, selDragEndY)
            val maxSY = maxOf(selDragStartY, selDragEndY)
            renderDashedRect(
                guiGraphics,
                minSX,
                minSY,
                maxSX,
                maxSY,
                0xCCFFFFFF.toInt(),
                mapLeft,
                mapTop,
                mapAreaW,
                mapAreaH
            )
        }
    }

    fun renderDashedRect(
        guiGraphics: GuiGraphics, minSX: Float, minSY: Float, maxSX: Float, maxSY: Float, color: Int,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int
    ) {
        val cx2 = mapLeft + mapAreaW
        val cy2 = mapTop + mapAreaH
        val dash = 4
        val gap = 3
        val dashLen = dash + gap
        // Top edge
        if (minSY.toInt() in mapTop until cy2) {
            val start = maxOf(minSX.toInt(), mapLeft)
            val end = minOf(maxSX.toInt(), cx2)
            var x = start
            while (x < end) {
                val ex = minOf(x + dash, end)
                guiGraphics.fill(
                    x,
                    minSY.toInt(),
                    ex,
                    minSY.toInt() + 1,
                    color
                )
                x += dashLen
            }
        }
        // Bottom edge
        if (maxSY.toInt() in mapTop until cy2) {
            val start = maxOf(minSX.toInt(), mapLeft)
            val end = minOf(maxSX.toInt(), cx2)
            var x = start
            while (x < end) {
                val ex = minOf(x + dash, end)
                guiGraphics.fill(
                    x,
                    maxSY.toInt(),
                    ex,
                    maxSY.toInt() + 1,
                    color
                )
                x += dashLen
            }
        }
        // Left edge
        if (minSX.toInt() in mapLeft until cx2) {
            val start = maxOf(minSY.toInt(), mapTop)
            val end = minOf(maxSY.toInt(), cy2)
            var y = start
            while (y < end) {
                val ey = minOf(y + dash, end)
                guiGraphics.fill(
                    minSX.toInt(),
                    y,
                    minSX.toInt() + 1,
                    ey,
                    color
                )
                y += dashLen
            }
        }
        // Right edge
        if (maxSX.toInt() in mapLeft until cx2) {
            val start = maxOf(minSY.toInt(), mapTop)
            val end = minOf(maxSY.toInt(), cy2)
            var y = start
            while (y < end) {
                val ey = minOf(y + dash, end)
                guiGraphics.fill(
                    maxSX.toInt(),
                    y,
                    maxSX.toInt() + 1,
                    ey,
                    color
                )
                y += dashLen
            }
        }
    }

    // ── Hit-testing ──

    fun screenRectFromBox(
        box: SelBox,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, zoom: Double
    ): Rect4f {
        val scale = CoordinateConverter.scaleFromZoom(zoom)
        val sx = CoordinateConverter.worldToScreenX(box.worldMinX, mapCenterX, viewBlockX, scale).toFloat()
        val ex = CoordinateConverter.worldToScreenX(box.worldMaxX, mapCenterX, viewBlockX, scale).toFloat()
        val sy = CoordinateConverter.worldToScreenY(box.worldMinZ, mapCenterY, viewBlockZ, scale).toFloat()
        val ey = CoordinateConverter.worldToScreenY(box.worldMaxZ, mapCenterY, viewBlockZ, scale).toFloat()
        return Rect4f(minOf(sx, ex), minOf(sy, ey), maxOf(sx, ex), maxOf(sy, ey))
    }

    fun hitTestBox(
        mouseX: Double, mouseY: Double,
        selBoxes: List<SelBox>,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, zoom: Double
    ): SelBox? {
        for (box in selBoxes) {
            val r = screenRectFromBox(box, viewBlockX, viewBlockZ, mapCenterX, mapCenterY, zoom)
            if (mouseX in r.minX.toDouble()..r.maxX.toDouble() &&
                mouseY in r.minY.toDouble()..r.maxY.toDouble()
            ) return box
        }
        return null
    }

    // ── Context menu ──

    fun renderContextMenu(
        guiGraphics: GuiGraphics, font: Font, mouseX: Int, mouseY: Int,
        selMenuX: Int, selMenuY: Int, screenWidth: Int, screenHeight: Int,
        isAdmin: Boolean, confirmClear: Boolean
    ) {
        val removeLabel = Component.translatable("context.superbwarfare.tactical_map.sel_menu.remove").string
        val clearLabel = Component.translatable("context.superbwarfare.tactical_map.sel_menu.clear").string
        val confirmClearLabel = "⚠ $clearLabel ?"

        val itemHeight = 14
        val itemCount = if (isAdmin) 2 else 1
        val clearW = if (confirmClear) font.width(confirmClearLabel) else font.width(clearLabel)
        val menuW = maxOf(font.width(removeLabel), if (isAdmin) clearW else 0) + 16
        val menuH = itemCount * itemHeight + 4

        var mx = selMenuX + 8
        var my = selMenuY
        if (mx + menuW > screenWidth) mx = selMenuX - menuW - 8
        if (my + menuH > screenHeight) my = screenHeight - menuH

        // Background
        guiGraphics.fill(mx, my, mx + menuW, my + menuH, 0xEE2A2A2A.toInt())
        guiGraphics.fill(mx, my, mx + menuW, my + 1, 0xFF555555.toInt())
        guiGraphics.fill(mx, my + menuH - 1, mx + menuW, my + menuH, 0xFF555555.toInt())
        guiGraphics.fill(mx, my, mx + 1, my + menuH, 0xFF555555.toInt())
        guiGraphics.fill(mx + menuW - 1, my, mx + menuW, my + menuH, 0xFF555555.toInt())

        // Item 0: Remove selection
        val ty0 = my + 2
        val hovered0 = mouseX in mx..mx + menuW && mouseY in ty0..ty0 + itemHeight
        if (hovered0) guiGraphics.fill(mx + 1, ty0, mx + menuW - 1, ty0 + itemHeight, 0x664444FF)
        guiGraphics.drawString(
            font, removeLabel, mx + 8, ty0 + 3,
            if (hovered0) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt(), false
        )

        // Item 1: Clear area (admin only)
        if (isAdmin) {
            val ty1 = my + 2 + itemHeight
            val hovered1 = mouseX in mx..mx + menuW && mouseY in ty1..ty1 + itemHeight
            val confirmBg = if (confirmClear) 0x66AA2222 else 0x66444444
            if (hovered1) guiGraphics.fill(mx + 1, ty1, mx + menuW - 1, ty1 + itemHeight, confirmBg)
            val label = if (confirmClear) confirmClearLabel else clearLabel
            val labelColor = when {
                confirmClear && hovered1 -> 0xFFFF2222.toInt()
                confirmClear -> 0xFFFF4444.toInt()
                hovered1 -> 0xFFFF5555.toInt()
                else -> 0xFFCC6666.toInt()
            }
            guiGraphics.drawString(font, label, mx + 8, ty1 + 3, labelColor, false)
        }
    }

    /**
     * Handles click on selection context menu.
     * @return Pair(clickConsumed, shouldToggleConfirmClear) — caller manages confirmClear state
     */
    fun handleMenuClick(
        mouseX: Double, mouseY: Double,
        selMenuX: Int, selMenuY: Int, screenWidth: Int, screenHeight: Int,
        isAdmin: Boolean, confirmClear: Boolean, font: Font
    ): Int { // 0=none, 1=remove, 2=clear
        val removeLabel = Component.translatable("context.superbwarfare.tactical_map.sel_menu.remove").string
        val clearLabel = Component.translatable("context.superbwarfare.tactical_map.sel_menu.clear").string
        val confirmClearLabel = "⚠ $clearLabel ?"

        val itemHeight = 14
        val itemCount = if (isAdmin) 2 else 1
        val clearW = if (confirmClear) font.width(confirmClearLabel) else font.width(clearLabel)
        val menuW = maxOf(font.width(removeLabel), if (isAdmin) clearW else 0) + 16
        val menuH = itemCount * itemHeight + 4

        var mx = selMenuX + 8
        var my = selMenuY
        if (mx + menuW > screenWidth) mx = selMenuX - menuW - 8
        if (my + menuH > screenHeight) my = screenHeight - menuH

        val ty0 = my + 2
        if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in ty0.toDouble()..(ty0 + itemHeight).toDouble())
            return 1

        if (isAdmin) {
            val ty1 = my + 2 + itemHeight
            if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in ty1.toDouble()..(ty1 + itemHeight).toDouble())
                return 2
        }
        return 0
    }
}
