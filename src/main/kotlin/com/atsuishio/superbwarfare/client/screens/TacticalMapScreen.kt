package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.map.TacticalMapCache
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.HangingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class TacticalMapScreen : Screen(Component.translatable("screen.superbwarfare.tactical_map")) {

    // Panel layout
    private var panelX = 0
    private var panelY = 0
    private var panelWidth = 0
    private var panelHeight = 0
    private var mapCenterX = 0f
    private var mapCenterY = 0f
    private var mapLeft = 0
    private var mapTop = 0
    private var mapAreaW = 0
    private var mapAreaH = 0

    // Map view center (world coordinates) — free pan, not tied to player
    private var viewBlockX = 0.0
    private var viewBlockZ = 0.0

    // Drag state
    private var isDragging = false
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0

    // Entity cache
    private var cachedFriendlyEntities: List<Entity> = emptyList()
    private var entityCacheTick = 0

    // Textures
    private val COMPASS_ROSE = loc("textures/overlay/tactical_map/compass_rose.png")
    private val PLAYER_MARKER = loc("textures/overlay/tactical_map/player_marker.png")
    private val TEAMMATE_MARKER = loc("textures/overlay/tactical_map/teammate_marker.png")

    // Settings
    private var zoom = 5.0

    // Context menu
    private var ctxMenuVisible = false
    private var ctxMenuX = 0
    private var ctxMenuY = 0
    private var ctxWorldX = 0
    private var ctxWorldY = 0
    private var ctxWorldZ = 0

    private data class ContextMenuItem(val label: String, val action: () -> Unit)

    override fun isPauseScreen() = false

    // Center-on-player button
    private val centerBtn: Button = Button.builder(Component.literal("⌖")) { centerOnPlayer() }
        .pos(0, 0)
        .size(20, 20)
        .build()

    override fun init() {
        zoom = DisplayConfig.TACTICAL_MAP_ZOOM.get().toDouble()
        val player = localPlayer
        if (player != null) {
            viewBlockX = player.x
            viewBlockZ = player.z
        }
        addRenderableWidget(centerBtn)
    }

    private fun centerOnPlayer() {
        val player = localPlayer ?: return
        viewBlockX = player.x
        viewBlockZ = player.z
        zoom = 10.0
    }

    override fun tick() {
        val player = localPlayer ?: return
        val level = player.level()

        TacticalMapCache.processChunkUpdates(level)

        // Periodic rescan: re-sample nearby chunks to catch block changes
        TacticalMapCache.periodicRescan(level, player.x, player.z)

        if (player.tickCount % 10 == 0) {
            TacticalMapCache.uploadDirtyTextures()
        }

        if (player.tickCount - entityCacheTick >= 10) {
            entityCacheTick = player.tickCount
            cachedFriendlyEntities = EntityFindUtil.getEntities(level).all
                .filter { entity ->
                    entity.isAlive
                            && entity !== player
                            && entity !is ItemEntity
                            && entity !is HangingEntity
                            && entity !is Projectile
                            && SeekTool.IS_FRIENDLY.test(player, entity)
                }
                .toList()
        }

    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        val player = localPlayer ?: return

        recomputeLayout()

        // Panel background
        renderPanelBg(pGuiGraphics)

        // === CLIP to map area ===
        pGuiGraphics.enableScissor(mapLeft, mapTop, mapLeft + mapAreaW, mapTop + mapAreaH)

        // Diagonal stripes on unexplored areas (render first, tiles cover them)
        renderStripes(pGuiGraphics)

        // Setup render state for textured quads
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO
        )

        // Layer 1: Map tiles (terrain)
        renderMapTiles(pGuiGraphics)

        // Layer 2: Grid lines
        renderGridLines(pGuiGraphics)

        // Layer 3: Friendly entity markers
        renderFriendlyMarkers(pGuiGraphics, player)

        // Layer 4: Player marker
        renderPlayerMarker(pGuiGraphics, player)

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        // === END CLIP ===
        pGuiGraphics.disableScissor()

        // Border frame around map area
        renderMapBorder(pGuiGraphics)

        // Outside clip: Compass rose and HUD
        renderCompassRose(pGuiGraphics)
        renderHudText(pGuiGraphics, player, pMouseX, pMouseY)

        renderContextMenu(pGuiGraphics, pMouseX, pMouseY)
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
    }

    // ========================
    //  Context menu
    // ========================

    private fun buildContextMenu(): List<ContextMenuItem> = listOf(
        ContextMenuItem("Teleport to ${ctxWorldX}, ${ctxWorldY}, ${ctxWorldZ}") {
            minecraft!!.player!!.connection.sendCommand("tp $ctxWorldX $ctxWorldY $ctxWorldZ")
        }
    )

    private fun renderContextMenu(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (!ctxMenuVisible) return

        val items = buildContextMenu()
        if (items.isEmpty()) return

        val font = minecraft!!.font
        val padding = 4
        val itemHeight = 12
        val menuW = items.maxOf { font.width(it.label) } + padding * 2
        val menuH = items.size * itemHeight + padding * 2 + 2

        // Position: offset from cursor, clamp to screen
        var mx = ctxMenuX + 8
        var my = ctxMenuY
        if (mx + menuW > width) mx = ctxMenuX - menuW - 8
        if (my + menuH > height) my = height - menuH - 4

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
            if (hovered) guiGraphics.fill(mx + 1, iy, mx + menuW - 1, iy + itemHeight, 0x664444FF.toInt())
            guiGraphics.drawString(font, item.label, mx + padding, iy + 2,
                if (hovered) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt(), false
            )
        }

        // Click handling: check if mouse clicked on an item
        // (we handle this in mouseClicked by checking ctxMenuVisible)
    }

    // ========================
    //  Layout
    // ========================

    private fun recomputeLayout() {
        panelWidth = (width * 0.55).toInt()
        panelHeight = (height * 0.85).toInt()
        panelX = width - panelWidth - 8
        panelY = 8
        mapAreaW = panelWidth - 20
        mapAreaH = panelHeight - 48 // bottom bar for HUD + buttons
        mapLeft = panelX + 10
        mapTop = panelY + 10
        mapCenterX = panelX + panelWidth / 2f
        mapCenterY = panelY + 10 + mapAreaH / 2f

        // Update center button position (only when layout changes)
        centerBtn.setX(panelX + 4)
        centerBtn.setY(panelY + panelHeight - 24)
    }

    private fun renderPanelBg(guiGraphics: GuiGraphics) {
        // Kraft paper background (military map style)
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFD4C8A8.toInt())
        // Outer border (80% black)
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xCC000000.toInt())
        guiGraphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xCC000000.toInt())
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xCC000000.toInt())
        guiGraphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xCC000000.toInt())
    }

    private fun renderStripes(guiGraphics: GuiGraphics) {
        // 45° diagonal stripes — rotated horizontal lines
        val stripeColor = 0x99B8A088.toInt()
        val spacing = 8
        val totalW = mapAreaW + mapAreaH
        val centerX = mapLeft + mapAreaW / 2f
        val centerY = mapTop + mapAreaH / 2f

        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.rotateAround(Axis.ZP.rotationDegrees(-45f), centerX, centerY, 0f)

        var y = -totalW
        while (y < totalW * 2) {
            val ly = centerY + y
            guiGraphics.fill(
                (centerX - totalW).toInt(), ly.toInt(),
                (centerX + totalW).toInt(), (ly + 2).toInt(),
                stripeColor
            )
            y += spacing
        }

        pose.popPose()
    }

    private fun renderMapBorder(guiGraphics: GuiGraphics) {
        val borderColor = 0xCC000000.toInt() // 80% black
        // Top
        guiGraphics.fill(mapLeft - 1, mapTop - 1, mapLeft + mapAreaW + 1, mapTop, borderColor)
        // Bottom
        guiGraphics.fill(mapLeft - 1, mapTop + mapAreaH, mapLeft + mapAreaW + 1, mapTop + mapAreaH + 1, borderColor)
        // Left
        guiGraphics.fill(mapLeft - 1, mapTop - 1, mapLeft, mapTop + mapAreaH + 1, borderColor)
        // Right
        guiGraphics.fill(mapLeft + mapAreaW, mapTop - 1, mapLeft + mapAreaW + 1, mapTop + mapAreaH + 1, borderColor)
    }

    // ========================
    //  Map tiles
    // ========================

    private fun renderMapTiles(guiGraphics: GuiGraphics) {
        val scale = zoom / 5.0
        val visibleBlocksX = (mapAreaW / scale).toInt()
        val visibleBlocksZ = (mapAreaH / scale).toInt()

        val tiles = TacticalMapCache.getVisibleTiles(
            viewBlockX.toInt(),
            viewBlockZ.toInt(),
            ((visibleBlocksX / 2.0 * 1.5).toInt()).coerceAtLeast(256)
        )

        for (tile in tiles) {
            val texLoc = TacticalMapCache.getTileTexture(tile.rx, tile.rz) ?: continue

            val tileBlockX = tile.rx.toDouble() * TacticalMapCache.TILE_SIZE
            val tileBlockZ = tile.rz.toDouble() * TacticalMapCache.TILE_SIZE

            // Compute screen rect in double precision, then expand by 1px
            // to prevent black seams from float→int truncation between tiles
            val x1 = mapCenterX + (tileBlockX - viewBlockX) * scale
            val y1 = mapCenterY + (tileBlockZ - viewBlockZ) * scale
            val x2 = x1 + TacticalMapCache.TILE_SIZE * scale
            val y2 = y1 + TacticalMapCache.TILE_SIZE * scale

            // Quick reject
            if (x2 < mapLeft || x1 > mapLeft + mapAreaW ||
                y2 < mapTop || y1 > mapTop + mapAreaH
            ) continue

            val ix = x1.toInt()
            val iy = y1.toInt()
            // +1 overlap on right/bottom edges fills the 1px gap from truncation
            val iw = maxOf(1, (x2 - x1).toInt() + 1)
            val ih = maxOf(1, (y2 - y1).toInt() + 1)

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            guiGraphics.blit(
                texLoc,
                ix, iy,
                iw, ih,
                0f, 0f,
                TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE,
                TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE
            )
        }
    }

    // ========================
    //  Grid
    // ========================

    private fun renderGridLines(guiGraphics: GuiGraphics) {
        val scale = zoom / 5.0
        val gridBlockInterval = getGridInterval()
        val gridPixelInterval = (gridBlockInterval * scale).toFloat()
        if (gridPixelInterval < 8) return

        val originBlockX = (viewBlockX / gridBlockInterval).toInt() * gridBlockInterval
        val originBlockZ = (viewBlockZ / gridBlockInterval).toInt() * gridBlockInterval
        val lineColor = 0x33000000.toInt()  // 20% black line
        val labelColor = 0xCC000000.toInt()  // 80% black label
        val font = minecraft!!.font

        // Vertical lines (constant X → longitude)
        var blockX = originBlockX - gridBlockInterval * ((mapAreaW / gridPixelInterval).toInt() + 2)
        while (true) {
            val screenX = (mapCenterX + ((blockX - viewBlockX) * scale).toFloat()).toInt()
            if (screenX > mapLeft + mapAreaW) break
            if (screenX >= mapLeft) {
                guiGraphics.fill(screenX, mapTop, screenX + 1, mapTop + mapAreaH, lineColor)
                // Label at top-right of line: "X:<value>"
                if (screenX + 2 < mapLeft + mapAreaW - 20)
                    guiGraphics.drawString(font, "${"%,d".format(blockX)}",
                        screenX + 2, mapTop + 2, labelColor, false)
            }
            blockX += gridBlockInterval
        }

        // Horizontal lines (constant Z → latitude)
        var blockZ = originBlockZ - gridBlockInterval * ((mapAreaH / gridPixelInterval).toInt() + 2)
        while (true) {
            val screenY = (mapCenterY + ((blockZ - viewBlockZ) * scale).toFloat()).toInt()
            if (screenY > mapTop + mapAreaH) break
            if (screenY >= mapTop) {
                guiGraphics.fill(mapLeft, screenY, mapLeft + mapAreaW, screenY + 1, lineColor)
                // Label at left-upper of line: "Z:<value>"
                if (screenY - 10 > mapTop)
                    guiGraphics.drawString(font, "${"%,d".format(blockZ)}",
                        mapLeft + 2, screenY - 10, labelColor, false)
            }
            blockZ += gridBlockInterval
        }
    }

    private fun getGridInterval(): Int = when {
        zoom > 2.0  -> 100
        zoom > 1.0  -> 200
        zoom >= 0.5 -> 250
        zoom >= 0.3 -> 500
        zoom >= 0.2 -> 1000
        zoom >= 0.15 -> 2000
        else        -> 5000
    }

    private fun gridLabel(): String = when {
        zoom > 2.0  -> "100m"
        zoom > 1.0  -> "200m"
        zoom >= 0.5 -> "250m"
        zoom >= 0.3 -> "500m"
        zoom >= 0.2 -> "1km"
        zoom >= 0.15 -> "2km"
        else        -> "5km"
    }

    // ========================
    //  Markers
    // ========================

    private fun renderFriendlyMarkers(guiGraphics: GuiGraphics, player: Player) {
        val scale = zoom / 5.0

        for (entity in cachedFriendlyEntities) {
            val dx = entity.x - viewBlockX
            val dz = entity.z - viewBlockZ
            val screenX = mapCenterX + (dx * scale).toFloat()
            val screenY = mapCenterY + (dz * scale).toFloat()
            val markerSize = when (entity) { is VehicleEntity -> 8f; is Player -> 7f; else -> 6f }

            // Edge clamp: if outside map, show at edge with reduced alpha
            val clampedX = screenX.coerceIn((mapLeft + 4).toFloat(), (mapLeft + mapAreaW - 4).toFloat())
            val clampedY = screenY.coerceIn((mapTop + 4).toFloat(), (mapTop + mapAreaH - 4).toFloat())
            val alpha = if (screenX == clampedX && screenY == clampedY) 1f else 0.5f

            RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
            guiGraphics.blit(
                TEAMMATE_MARKER,
                (clampedX - markerSize / 2).toInt(), (clampedY - markerSize / 2).toInt(),
                0f, 0f, markerSize.toInt(), markerSize.toInt(), markerSize.toInt(), markerSize.toInt()
            )
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }

    private fun renderPlayerMarker(guiGraphics: GuiGraphics, player: Player) {
        val scale = zoom / 5.0
        val px = mapCenterX + ((player.x - viewBlockX) * scale).toFloat()
        val py = mapCenterY + ((player.z - viewBlockZ) * scale).toFloat()

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()
        poseStack.translate(px, py, 0f)
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(player.yRot + 180f), 0f, 0f, 0f
        )
        guiGraphics.blit(PLAYER_MARKER, -6, -6, 0f, 0f, 12, 12, 12, 12)
        poseStack.popPose()
    }

    // ========================
    //  HUD elements (outside clip)
    // ========================

    private fun renderCompassRose(guiGraphics: GuiGraphics) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f)
        guiGraphics.blit(COMPASS_ROSE, panelX + 6, panelY + 8, 0f, 0f, 32, 32, 32, 32)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    private fun renderHudText(guiGraphics: GuiGraphics, player: Player, mouseX: Int, mouseY: Int) {
        val font = minecraft!!.font
        val barTop = panelY + panelHeight - 24
        val scale = zoom / 5.0

        // Mouse world coordinates (or --- if outside map area)
        val posText = if (mouseX in mapLeft..mapLeft + mapAreaW && mouseY in mapTop..mapTop + mapAreaH) {
            val wx = (viewBlockX + (mouseX - mapCenterX) / scale).toInt()
            val wz = (viewBlockZ + (mouseY - mapCenterY) / scale).toInt()
            val level = player.level()
            val chunk = level.getChunk(wx shr 4, wz shr 4)
            val chunkLoaded = chunk is LevelChunk && !chunk.isEmpty
            val wy = when {
                chunkLoaded -> {
                    val h = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, wx, wz) - 1
                    if (h > level.minBuildHeight) h.toString() else "---"
                }
                else -> TacticalMapCache.getCachedHeight(wx, wz)?.let { (it - 1).toString() } ?: "---"
            }
            "$wx, $wy, $wz"
        } else {
            "---, ---, ---"
        }
        guiGraphics.drawString(font,
            "POS: $posText",
            panelX + 42, barTop, 0xCC000000.toInt(), false
        )
        val heading = ((player.yRot % 360 + 360) % 360).toInt()
        val dir = when {
            heading in 315..360 || heading in 0..44 -> "N"
            heading in 45..134 -> "E"
            heading in 135..224 -> "S"
            heading in 225..314 -> "W"
            else -> "?"
        }
        guiGraphics.drawString(font,
            "HDG: ${heading}° $dir  |  %.1fx | Grid: %s".format(zoom, gridLabel()),
            panelX + 42, barTop + 10, 0xCC000000.toInt(), false
        )
    }

    // ========================
    //  Mouse input (pan + zoom)
    // ========================

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        // Context menu item click
        if (ctxMenuVisible && pButton == 0) {
            val items = buildContextMenu()
            val font = minecraft!!.font
            val padding = 4; val itemHeight = 12
            val menuW = items.maxOf { font.width(it.label) } + padding * 2
            val menuH = items.size * itemHeight + padding * 2 + 2
            var mx = ctxMenuX + 8; var my = ctxMenuY
            if (mx + menuW > width) mx = ctxMenuX - menuW - 8
            if (my + menuH > height) my = height - menuH - 4

            for ((i, item) in items.withIndex()) {
                val iy = my + padding + i * itemHeight
                if (pMouseX in mx.toDouble()..(mx + menuW).toDouble() && pMouseY in iy.toDouble()..(iy + itemHeight).toDouble()) {
                    item.action()
                    ctxMenuVisible = false
                    return true
                }
            }
            ctxMenuVisible = false
            return true
        }
        if (pButton == 0 && isMouseInPanel(pMouseX, pMouseY)) {
            isDragging = true
            lastMouseX = pMouseX
            lastMouseY = pMouseY
            return true
        }
        if (pButton == 1 && isMouseInPanel(pMouseX, pMouseY)) {
            // Right-click: open context menu
            val scale = zoom / 5.0
            ctxWorldX = (viewBlockX + (pMouseX - mapCenterX) / scale).toInt()
            ctxWorldZ = (viewBlockZ + (pMouseY - mapCenterY) / scale).toInt()
            val level = minecraft!!.player!!.level()
            val chunk = level.getChunk(ctxWorldX shr 4, ctxWorldZ shr 4)
            val chunkLoaded = chunk is LevelChunk && !chunk.isEmpty
            ctxWorldY = if (chunkLoaded) {
                level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, ctxWorldX, ctxWorldZ)
            } else {
                val cached = TacticalMapCache.getCachedHeight(ctxWorldX, ctxWorldZ)
                if (cached != null) cached.toInt() else minecraft!!.player!!.blockY
            }
            ctxMenuX = pMouseX.toInt()
            ctxMenuY = pMouseY.toInt()
            ctxMenuVisible = true
            return true
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        if (isDragging && pButton == 0) {
            val scale = zoom / 5.0
            viewBlockX -= (pMouseX - lastMouseX) / scale
            viewBlockZ -= (pMouseY - lastMouseY) / scale
            lastMouseX = pMouseX
            lastMouseY = pMouseY
            return true
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
    }

    override fun mouseReleased(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        if (pButton == 0) {
            isDragging = false
            return true
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton)
    }

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, pScroll: Double): Boolean {
        if (isMouseInPanel(pMouseX, pMouseY)) {
            zoom = (zoom * (1.0 + pScroll * 0.15)).coerceIn(0.05, 20.0)
            DisplayConfig.TACTICAL_MAP_ZOOM.set(zoom)
            return true
        }
        return super.mouseScrolled(pMouseX, pMouseY, pScroll)
    }

    private fun isMouseInPanel(mx: Double, my: Double): Boolean {
        return mx >= mapLeft && mx <= mapLeft + mapAreaW && my >= mapTop && my <= mapTop + mapAreaH
    }

    // ========================
    //  Keyboard
    // ========================

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        if (pKeyCode == ModKeyMappings.TOGGLE_TACTICAL_MAP.key.value ||
            pKeyCode == 256 // ESC
        ) {
            onClose()
            return true
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers)
    }

    override fun onClose() {
        super.onClose()
    }

    override fun renderBackground(pGuiGraphics: GuiGraphics) {
        // Don't darken — game keeps rendering behind
    }
}
