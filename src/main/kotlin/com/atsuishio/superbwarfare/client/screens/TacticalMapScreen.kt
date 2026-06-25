package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.map.TacticalMapCache
import com.atsuishio.superbwarfare.client.map.context.MapContextMenu
import com.atsuishio.superbwarfare.client.map.context.MapMarker
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModTags
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
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.io.File
import java.util.*
import kotlin.math.atan2

@OnlyIn(Dist.CLIENT)
class TacticalMapScreen : Screen(Component.translatable("screen.superbwarfare.tactical_map")) {

    companion object {
        private var savedFollowPlayer = false

        // Textures
        private val COMPASS_ROSE = loc("textures/overlay/tactical_map/compass_rose.png")
        private val PLAYER_MARKER = loc("textures/overlay/tactical_map/player_marker.png")
        private val TEAMMATE_MARKER = loc("textures/overlay/tactical_map/teammate_marker.png")
        private val POSITION_MARKER = loc("textures/overlay/tactical_map/position_marker.png")

        // Vehicle icons
        private val ICON_AIRCRAFT = loc("textures/overlay/tactical_map/vehicle/aircraft.png")
        private val ICON_HELICOPTER = loc("textures/overlay/tactical_map/vehicle/helicopter.png")
        private val ICON_TANK = loc("textures/overlay/tactical_map/vehicle/tank.png")
        private val ICON_APC = loc("textures/overlay/tactical_map/vehicle/apc.png")
        private val ICON_AA = loc("textures/overlay/tactical_map/vehicle/aa.png")
        private val ICON_CAR = loc("textures/overlay/tactical_map/vehicle/car.png")
        private val ICON_ARTILLERY = loc("textures/overlay/tactical_map/vehicle/artillery.png")
        private val ICON_DRONE = loc("textures/overlay/tactical_map/vehicle/drone.png")
        private val ICON_BOAT = loc("textures/overlay/tactical_map/vehicle/boat.png")
        private val ICON_DEFENSE = loc("textures/overlay/tactical_map/vehicle/defense.png")
        private val ICON_AIRSHIP = loc("textures/overlay/tactical_map/vehicle/airship.png")
        private val ICON_MINE = loc("textures/overlay/tactical_map/vehicle/mine.png")
        private val ICON_MISSILE = loc("textures/overlay/tactical_map/vehicle/missile.png")
        private val ICON_MAID = loc("textures/overlay/tactical_map/vehicle/maid.png")
    }

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

    // Settings
    private var zoom = 5.0

    // Markers
    private val markers: MutableList<MapMarker> = mutableListOf()
    private val connections: MutableMap<UUID, MutableSet<UUID>> = mutableMapOf()
    private var draggingMarker: MapMarker? = null
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0

    // Connection mode
    private var connectionMode = false
    private var connectingFrom: MapMarker? = null

    // Line context menu
    private var ctxLinePair: Pair<MapMarker, MapMarker>? = null
    private var ctxLineMenuX = 0
    private var ctxLineMenuY = 0

    // Hovered line for highlight hovered marker for Delete key
    private var hoveredLine: Pair<MapMarker, MapMarker>? = null
    private var hoveredMarker: MapMarker? = null

    // Context menu (delegated)
    private lateinit var contextMenu: MapContextMenu

    private var markersLoaded = false

    override fun isPauseScreen() = false

    // Center-on-player button
    private val centerBtn: Button = Button.builder(Component.literal("⌖")) { centerOnPlayer() }
        .pos(0, 0)
        .size(20, 20)
        .build()

    // Follow-player toggle button
    private var followPlayer = false
    private var lastFollowState = false

    private val followIconNormal = Component.literal("◉")
    private val followIconActive = Component.literal("◉").copy().withStyle(net.minecraft.ChatFormatting.GREEN)

    private val followBtn: Button = Button.builder(followIconNormal) { toggleFollow() }
        .pos(0, 0)
        .size(20, 20)
        .build()

    private fun toggleFollow() {
        followPlayer = !followPlayer
        savedFollowPlayer = followPlayer
        if (followPlayer) {
            val player = localPlayer ?: return
            viewBlockX = player.x
            viewBlockZ = player.z
        }
    }

    override fun init() {
        zoom = DisplayConfig.TACTICAL_MAP_ZOOM.get().toDouble()
        followPlayer = savedFollowPlayer

        contextMenu = MapContextMenu()
        contextMenu.onMarkerCreated = {
            markers.add(it)
            saveMarker(it)
        }
        contextMenu.onMarkerEdited = { edited ->
            val idx = markers.indexOfFirst { m -> m.id == edited.id }
            if (idx >= 0) {
                val updated = MapMarker(edited.id, edited.name, edited.x, edited.y, edited.z, edited.colorIndex)
                markers[idx] = updated
                saveMarker(updated)
            }
        }
        contextMenu.onMarkerDelete = { marker ->
            // 清理双向连线：从对方文件中移除自己的 UUID
            val myConns = connections[marker.id] ?: emptySet()
            for (otherId in myConns) {
                val otherConns = connections[otherId]
                if (otherConns != null && otherConns.remove(marker.id)) {
                    val otherMarker = markers.find { m -> m.id == otherId }
                    if (otherMarker != null) saveMarker(otherMarker)
                }
            }
            connections.remove(marker.id)
            markers.remove(marker)
            deleteMarkerFile(marker)
        }
        contextMenu.onConnectRequested = { marker ->
            connectionMode = true
            connectingFrom = marker
        }

        lastFollowState = followPlayer
        followBtn.message = if (followPlayer) followIconActive else followIconNormal
        val player = localPlayer
        if (player != null) {
            viewBlockX = player.x
            viewBlockZ = player.z
        }
        addRenderableWidget(centerBtn)
        addRenderableWidget(followBtn)
    }

    // ── Marker persistence: <gameDir>/tactical_markers/<worldId>/<dim>/<uuid>.txt ──

    private fun getMarkerDir(): File {
        val worldId = TacticalMapCache.getWorldIdentifier()
        val dim = (minecraft?.level?.dimension()?.location()?.toString() ?: "unknown").replace(":", "_")
        val dir = File(minecraft!!.gameDirectory, "superbwarfare/tactical_markers/$worldId/$dim")
        dir.mkdirs()
        return dir
    }

    private fun markerFile(uuid: UUID): File {
        return File(getMarkerDir(), "$uuid.txt")
    }

    private fun loadMarkers() {
        val dir = getMarkerDir()
        val files = dir.listFiles() ?: return
        markers.clear()
        for (file in files) {
            if (!file.isFile || !file.name.endsWith(".txt")) continue
            try {
                val uuid = UUID.fromString(file.nameWithoutExtension)
                val p = file.readText().trim().split("|", limit = 6)
                if (p.size >= 5) {
                    markers.add(
                        MapMarker(
                            id = uuid,
                            name = p[0],
                            x = p[1].toInt(),
                            y = p[2].toInt(),
                            z = p[3].toInt(),
                            colorIndex = p[4].toInt()
                        )
                    )
                    if (p.size >= 6 && p[5].isNotEmpty()) {
                        connections[uuid] = p[5].split(",").mapNotNullTo(mutableSetOf()) { s ->
                            try {
                                UUID.fromString(s)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveMarker(marker: MapMarker) {
        try {
            val conns = connections[marker.id]?.joinToString(",") ?: ""
            val content = "${marker.name}|${marker.x}|${marker.y}|${marker.z}|${marker.colorIndex}|$conns"
            val newFile = markerFile(marker.id)
            val tmp = File(newFile.parentFile, "${newFile.name}.tmp")
            newFile.delete()
            tmp.writeText(content)
            tmp.renameTo(newFile)
        } catch (_: Exception) {
        }
    }

    private fun deleteMarkerFile(marker: MapMarker) {
        try {
            markerFile(marker.id).delete()
        } catch (_: Exception) {
        }
    }

    private fun centerOnPlayer() {
        followPlayer = false
        savedFollowPlayer = false
        val player = localPlayer ?: return
        viewBlockX = player.x
        viewBlockZ = player.z
        zoom = 10.0
    }

    override fun tick() {
        if (::contextMenu.isInitialized) contextMenu.editBoxTick()

        val player = localPlayer ?: return
        val level = player.level()

        // 从磁盘加载待处理的区块，按距离玩家最近优先
        TacticalMapCache.processPendingChunks(viewBlockX, viewBlockZ, 24)

        TacticalMapCache.processChunkUpdates(level, viewBlockX, viewBlockZ)

        // Periodic rescan: re-sample nearby chunks to catch block changes
        TacticalMapCache.periodicRescan(level, viewBlockX, viewBlockZ)

        // Upload dirty tile textures every tick so center-out loading is visible
        TacticalMapCache.uploadDirtyTextures()

        if (!markersLoaded) {
            loadMarkers()
            markersLoaded = true
        }

        // Follow player mode
        if (followPlayer) {
            viewBlockX = player.x
            viewBlockZ = player.z
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

        // Layer 3.5: Connection lines + preview in connection mode
        renderConnectionLines(pGuiGraphics, pMouseX, pMouseY)
        if (connectionMode && connectingFrom != null && isMouseInPanel(pMouseX.toDouble(), pMouseY.toDouble())) {
            renderConnectionPreview(pGuiGraphics, pMouseX, pMouseY)
        }

        // Layer 3.6: Position markers
        renderPositionMarkers(pGuiGraphics)

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

        // Connection mode indicator
        if (connectionMode) {
            val font = minecraft!!.font
            val txt = Component.translatable("hud.superbwarfare.tactical_map.connect_mode").string
            pGuiGraphics.drawString(font, txt, mapLeft, mapTop + mapAreaH + 3 + 10, 0xFFFFAA00.toInt(), false)
        }

        // Marker hover tooltip (coordinates only, also shown while dragging)
        if (!contextMenu.ctxMenuVisible && !contextMenu.editPanelVisible) {
            val s = zoom / 5.0
            val dm = draggingMarker
            if (dm != null) {
                // 拖动中：直接显示被拖标记点的实时坐标
                pGuiGraphics.renderTooltip(
                    font, listOf(
                        Component.literal("${dm.x}, ${dm.y}, ${dm.z}").withStyle(net.minecraft.ChatFormatting.YELLOW)
                    ), Optional.empty(), pMouseX, pMouseY
                )
            } else {
                val hm = contextMenu.hitTestMarker(
                    markers,
                    pMouseX.toDouble(),
                    pMouseY.toDouble(),
                    viewBlockX,
                    viewBlockZ,
                    s,
                    mapCenterX,
                    mapCenterY
                )
                hoveredMarker = hm
                if (hm != null) {
                    pGuiGraphics.renderTooltip(
                        font, listOf(
                            Component.literal("${hm.x}, ${hm.y}, ${hm.z}").withStyle(net.minecraft.ChatFormatting.GRAY)
                        ), Optional.empty(), pMouseX, pMouseY
                    )
                }
            }
        }

        // Line context menu (position frozen at click time)
        if (ctxLinePair != null) {
            val label = Component.translatable("context.superbwarfare.tactical_map.disconnect").string
            val pw = font.width(label) + 8
            val ph = 14
            pGuiGraphics.fill(ctxLineMenuX, ctxLineMenuY, ctxLineMenuX + pw, ctxLineMenuY + ph, 0xEE2A2A2A.toInt())
            pGuiGraphics.drawString(font, label, ctxLineMenuX + 4, ctxLineMenuY + 3, 0xFFFF5555.toInt(), false)
        }

        contextMenu.render(pGuiGraphics, font, pMouseX, pMouseY, width, height)

        // Update follow button visual state (only when changed to avoid input glitches)
        if (followPlayer != lastFollowState) {
            lastFollowState = followPlayer
            followBtn.message = if (followPlayer) followIconActive else followIconNormal
        }

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)

        // Button tooltips (must render after buttons)
        val font = minecraft!!.font
        if (centerBtn.isMouseOver(pMouseX.toDouble(), pMouseY.toDouble())) {
            pGuiGraphics.renderTooltip(
                font,
                listOf(Component.translatable("hud.superbwarfare.tactical_map.center_tooltip")),
                Optional.empty(),
                pMouseX, pMouseY
            )
        }
        if (followBtn.isMouseOver(pMouseX.toDouble(), pMouseY.toDouble())) {
            pGuiGraphics.renderTooltip(
                font,
                listOf(Component.translatable("hud.superbwarfare.tactical_map.follow_tooltip")),
                Optional.empty(),
                pMouseX, pMouseY
            )
        }
    }

    private fun renderPositionMarkers(guiGraphics: GuiGraphics) {
        val scale = zoom / 5.0
        val font = minecraft!!.font
        for (marker in markers) {
            MapContextMenu.renderMarker(
                guiGraphics, font, marker,
                viewBlockX, viewBlockZ, scale,
                mapCenterX, mapCenterY,
                mapLeft, mapTop, mapAreaW, mapAreaH,
                POSITION_MARKER,
                isDragging = marker == draggingMarker
            )
        }
    }

    private fun getValidConnections(): Set<Pair<MapMarker, MapMarker>> {
        val result = mutableSetOf<Pair<MapMarker, MapMarker>>()
        for (a in markers) {
            val aConns = connections[a.id] ?: continue
            for (bId in aConns) {
                val b = markers.find { it.id == bId } ?: continue
                val bConns = connections[bId] ?: continue
                if (bConns.contains(a.id) && a.id < bId) {
                    result.add(a to b)
                }
            }
        }
        return result
    }

    private fun renderConnectionLines(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val scale = zoom / 5.0
        val font = minecraft!!.font
        val color = 0xFFFFFFFF.toInt()

        hoveredLine = null
        for ((a, b) in getValidConnections()) {
            val ax = (mapCenterX + (a.x - viewBlockX) * scale).toFloat()
            val ay = (mapCenterY + (a.z - viewBlockZ) * scale).toFloat()
            val bx = (mapCenterX + (b.x - viewBlockX) * scale).toFloat()
            val by = (mapCenterY + (b.z - viewBlockZ) * scale).toFloat()

            // Check hover (suppressed when mouse is over a marker)
            val isHovered = hoveredMarker == null && hitTestLine(
                mouseX.toDouble(),
                mouseY.toDouble(),
                ax.toDouble(),
                ay.toDouble(),
                bx.toDouble(),
                by.toDouble()
            )
            if (isHovered) hoveredLine = a to b
            val lineColor = if (isHovered) 0xFFFFFFFF.toInt() else 0xCCFFAA00.toInt()

            // ── Smooth 1px line via PoseStack rotation ──
            val mx = (ax + bx) / 2f
            val my = (ay + by) / 2f
            val dx = (bx - ax).toDouble()
            val dy = (by - ay).toDouble()
            val len = kotlin.math.sqrt(dx * dx + dy * dy).toFloat()
            val angle = atan2(dy, dx)

            val pose = guiGraphics.pose()
            pose.pushPose()
            pose.translate(mx, my, 0f)
            pose.rotateAround(Axis.ZP.rotationDegrees(Math.toDegrees(angle).toFloat()), 0f, 0f, 0f)
            // 1px thin line
            guiGraphics.fill((-len / 2f).toInt(), 0, (len / 2f).toInt(), 1, lineColor)
            pose.popPose()

            // ── Distance label always above the line ──
            val dist = kotlin.math.sqrt(((a.x - b.x).toDouble() * (a.x - b.x) + (a.z - b.z) * (a.z - b.z)))
            val label = "${dist.toInt()}m"
            // 文字角度：水平阅读方向，始终在线上方
            val drawAngle = if (angle > Math.PI / 2 || angle < -Math.PI / 2) angle + Math.PI else angle
            // 垂直于线段向上偏移（屏幕 Y 轴向下，取正偏移 = 文字在线段上方）
            val perpUp = font.lineHeight / 2f + 2f

            pose.pushPose()
            pose.translate(
                (mx + kotlin.math.sin(angle) * perpUp).toFloat(),
                (my - kotlin.math.cos(angle) * perpUp).toFloat(),
                0f
            )
            pose.rotateAround(Axis.ZP.rotationDegrees(Math.toDegrees(drawAngle).toFloat()), 0f, 0f, 0f)
            val tw = font.width(label)
            guiGraphics.drawString(font, label, -tw / 2, -font.lineHeight / 2, color, false)
            pose.popPose()
        }
    }

    private fun renderConnectionPreview(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val scale = zoom / 5.0
        val src = connectingFrom ?: return
        val sx = (mapCenterX + (src.x - viewBlockX) * scale)
        val sy = (mapCenterY + (src.z - viewBlockZ) * scale)
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()
        val dx = mx - sx
        val dy = my - sy
        val len = kotlin.math.sqrt(dx * dx + dy * dy).toFloat()
        if (len < 2f) return
        val angle = atan2(dy, dx)
        val midX = ((sx + mx) / 2f).toFloat()
        val midY = ((sy + my) / 2f).toFloat()

        // Dashed line via PoseStack rotation: 1px segments with 1px gaps
        val dashColor = 0xAAFFAA00.toInt()
        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(midX, midY, 0f)
        pose.rotateAround(Axis.ZP.rotationDegrees(Math.toDegrees(angle).toFloat()), 0f, 0f, 0f)
        val halfLen = (len / 2f).toInt()
        var x = -halfLen
        while (x < halfLen) {
            guiGraphics.fill(x, 0, x + 1, 1, dashColor)
            x += 2  // 1px on, 1px off
        }
        pose.popPose()

        // Distance tooltip at mouse
        val worldMX = viewBlockX + (mouseX - mapCenterX) / scale
        val worldMZ = viewBlockZ + (mouseY - mapCenterY) / scale
        val realDist = kotlin.math.sqrt((src.x - worldMX) * (src.x - worldMX) + (src.z - worldMZ) * (src.z - worldMZ))
        val font = minecraft!!.font
        val label = "${realDist.toInt()}m"
        guiGraphics.drawString(font, label, mouseX + 10, mouseY + 6, 0xFFFFAA00.toInt(), true)
    }

    private fun hitTestLine(mx: Double, my: Double, x1: Double, y1: Double, x2: Double, y2: Double): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return false
        var t = ((mx - x1) * dx + (my - y1) * dy) / lenSq
        t = t.coerceIn(0.0, 1.0)
        val px = x1 + t * dx
        val py = y1 + t * dy
        return kotlin.math.hypot(mx - px, my - py) <= 5.0
    }

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
        centerBtn.x = mapLeft
        centerBtn.y = mapTop + mapAreaH + 14
        followBtn.x = mapLeft + 22
        followBtn.y = mapTop + mapAreaH + 14
    }

    private fun renderPanelBg(guiGraphics: GuiGraphics) {
        // Kraft paper background (military map style)
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFFD4C8A8.toInt())
        // Outer border (80% black)
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, 0xCC000000.toInt())
        guiGraphics.fill(
            panelX,
            panelY + panelHeight - 1,
            panelX + panelWidth,
            panelY + panelHeight,
            0xCC000000.toInt()
        )
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, 0xCC000000.toInt())
        guiGraphics.fill(
            panelX + panelWidth - 1,
            panelY,
            panelX + panelWidth,
            panelY + panelHeight,
            0xCC000000.toInt()
        )
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

    private fun renderMapTiles(guiGraphics: GuiGraphics) {
        val scale = zoom / 5.0
        val visibleBlocksX = (mapAreaW / scale).toInt()
        val visibleBlocksZ = (mapAreaH / scale).toInt()
        val radius = ((visibleBlocksX / 2.0 * 1.5).toInt()).coerceAtLeast(256)

        val factor = TacticalMapCache.computeLodMergeFactor(zoom)

        // Use a single PoseStack translate+scale for full float-precision tile positioning.
        // This eliminates the jitter that occurs when tiles are only a few pixels wide
        // and int-truncated coordinates cause them to snap between pixel boundaries.
        val originScreenX = mapCenterX - viewBlockX * scale
        val originScreenZ = mapCenterY - viewBlockZ * scale

        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(originScreenX.toFloat(), originScreenZ.toFloat(), 0f)
        pose.scale(scale.toFloat(), scale.toFloat(), 1f)

        if (factor == 1) {
            // ── Native tile path (zoom >= 2.0) ──
            val tiles = TacticalMapCache.getVisibleTiles(
                viewBlockX.toInt(), viewBlockZ.toInt(), radius
            )
            for (tile in tiles) {
                val texLoc = TacticalMapCache.getTileTexture(tile.rx, tile.rz) ?: continue

                val wx = tile.rx * TacticalMapCache.TILE_SIZE
                val wz = tile.rz * TacticalMapCache.TILE_SIZE
                val wx2 = wx + TacticalMapCache.TILE_SIZE
                val wz2 = wz + TacticalMapCache.TILE_SIZE

                // Quick reject in world space
                val visMinX = viewBlockX - visibleBlocksX / 2.0 - TacticalMapCache.TILE_SIZE
                val visMaxX = viewBlockX + visibleBlocksX / 2.0 + TacticalMapCache.TILE_SIZE
                val visMinZ = viewBlockZ - visibleBlocksZ / 2.0 - TacticalMapCache.TILE_SIZE
                val visMaxZ = viewBlockZ + visibleBlocksZ / 2.0 + TacticalMapCache.TILE_SIZE
                if (wx2 < visMinX || wx > visMaxX || wz2 < visMinZ || wz > visMaxZ) continue

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                guiGraphics.blit(
                    texLoc, wx, wz,
                    TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE,
                    0f, 0f,
                    TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE,
                    TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE
                )
            }
        } else {
            // ── LOD tile path (zoom < 2.0) ──
            val lodSize = TacticalMapCache.TILE_SIZE * factor
            val lodTiles = TacticalMapCache.getVisibleLodTiles(
                viewBlockX.toInt(), viewBlockZ.toInt(),
                radius.coerceAtLeast(lodSize), factor
            )
            for (lodTile in lodTiles) {
                val texLoc = TacticalMapCache.getLodTileTexture(
                    lodTile.factor, lodTile.rx, lodTile.rz
                ) ?: continue

                val wx = lodTile.rx * lodSize
                val wz = lodTile.rz * lodSize
                val wx2 = wx + lodSize
                val wz2 = wz + lodSize

                // Quick reject in world space
                val visMinX = viewBlockX - visibleBlocksX / 2.0 - lodSize
                val visMaxX = viewBlockX + visibleBlocksX / 2.0 + lodSize
                val visMinZ = viewBlockZ - visibleBlocksZ / 2.0 - lodSize
                val visMaxZ = viewBlockZ + visibleBlocksZ / 2.0 + lodSize
                if (wx2 < visMinX || wx > visMaxX || wz2 < visMinZ || wz > visMaxZ) continue

                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                guiGraphics.blit(
                    texLoc, wx, wz,
                    lodSize, lodSize,
                    0f, 0f,
                    TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE,
                    TacticalMapCache.TILE_SIZE, TacticalMapCache.TILE_SIZE
                )
            }
        }

        pose.popPose()
    }

    private fun renderGridLines(guiGraphics: GuiGraphics) {
        val scale = zoom / 5.0
        val gridBlockInterval = getGridInterval()
        val gridPixelInterval = (gridBlockInterval * scale).toFloat()
        if (gridPixelInterval < 8) return

        val originBlockX = (viewBlockX / gridBlockInterval).toInt() * gridBlockInterval
        val originBlockZ = (viewBlockZ / gridBlockInterval).toInt() * gridBlockInterval
        val lineColor = 0x66000000  // 40% black — main grid
        val labelColor = 0xCC000000.toInt()  // 80% black label
        val font = minecraft!!.font

        // World-to-screen origin for float-precision positioning
        val originScreenX = mapCenterX - viewBlockX * scale
        val originScreenZ = mapCenterY - viewBlockZ * scale

        // Vertical lines (constant X → longitude)
        var blockX = originBlockX - gridBlockInterval * ((mapAreaW / gridPixelInterval).toInt() + 2)
        while (true) {
            // Float-precision screen position — no int truncation
            val screenXf = originScreenX + blockX * scale
            if (screenXf > mapLeft + mapAreaW) break
            if (screenXf >= mapLeft) {
                val pose = guiGraphics.pose()
                pose.pushPose()
                pose.translate(screenXf.toFloat(), 0f, 0f)
                // Fill at relative (0, mapTop) — GPU places it precisely at screenXf
                guiGraphics.fill(0, mapTop, 1, mapTop + mapAreaH, lineColor)
                if (screenXf + 22 < mapLeft + mapAreaW)
                    guiGraphics.drawString(
                        font, "%,d".format(blockX),
                        2, mapTop + 2, labelColor, false
                    )
                pose.popPose()
            }
            blockX += gridBlockInterval
        }

        // Horizontal lines (constant Z → latitude)
        var blockZ = originBlockZ - gridBlockInterval * ((mapAreaH / gridPixelInterval).toInt() + 2)
        while (true) {
            // Float-precision screen position — no int truncation
            val screenZf = originScreenZ + blockZ * scale
            if (screenZf > mapTop + mapAreaH) break
            if (screenZf >= mapTop) {
                val pose = guiGraphics.pose()
                pose.pushPose()
                pose.translate(0f, screenZf.toFloat(), 0f)
                // Fill at relative (mapLeft, 0) — GPU places it precisely at screenZf
                guiGraphics.fill(mapLeft, 0, mapLeft + mapAreaW, 1, lineColor)
                if (screenZf - 10 > mapTop)
                    guiGraphics.drawString(
                        font, "%,d".format(blockZ),
                        mapLeft + 2, -10, labelColor, false
                    )
                pose.popPose()
            }
            blockZ += gridBlockInterval
        }

        // Chunk borderlines (every 16 blocks) — visible only at high zoom
        if (zoom > 15.0) {
            val chunkColor = 0x33000000  // 20% black — same as old grid line transparency
            val chunkInterval = 16
            val chunkOriginX = (viewBlockX / chunkInterval).toInt() * chunkInterval
            val chunkOriginZ = (viewBlockZ / chunkInterval).toInt() * chunkInterval

            // Vertical chunk borders
            var cx = chunkOriginX - chunkInterval * ((mapAreaW / (chunkInterval * scale)).toInt() + 2)
            while (true) {
                val screenXf = originScreenX + cx * scale
                if (screenXf > mapLeft + mapAreaW) break
                if (screenXf >= mapLeft) {
                    val pose = guiGraphics.pose()
                    pose.pushPose()
                    pose.translate(screenXf.toFloat(), 0f, 0f)
                    guiGraphics.fill(0, mapTop, 1, mapTop + mapAreaH, chunkColor)
                    pose.popPose()
                }
                cx += chunkInterval
            }

            // Horizontal chunk borders
            var cz = chunkOriginZ - chunkInterval * ((mapAreaH / (chunkInterval * scale)).toInt() + 2)
            while (true) {
                val screenZf = originScreenZ + cz * scale
                if (screenZf > mapTop + mapAreaH) break
                if (screenZf >= mapTop) {
                    val pose = guiGraphics.pose()
                    pose.pushPose()
                    pose.translate(0f, screenZf.toFloat(), 0f)
                    guiGraphics.fill(mapLeft, 0, mapLeft + mapAreaW, 1, chunkColor)
                    pose.popPose()
                }
                cz += chunkInterval
            }
        }
    }

    private fun getGridInterval(): Int = when {
        zoom > 2.0 -> 100
        zoom > 1.0 -> 200
        zoom >= 0.5 -> 250
        zoom >= 0.3 -> 500
        zoom >= 0.2 -> 1000
        zoom >= 0.15 -> 2000
        else -> 5000
    }

    private fun gridLabel(): String = when {
        zoom > 2.0 -> "100m"
        zoom > 1.0 -> "200m"
        zoom >= 0.5 -> "250m"
        zoom >= 0.3 -> "500m"
        zoom >= 0.2 -> "1km"
        zoom >= 0.15 -> "2km"
        else -> "5km"
    }

    private fun renderFriendlyMarkers(guiGraphics: GuiGraphics, player: Player) {
        val scale = zoom / 5.0
        val level = player.level()

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

        val all = (ClientSyncedEntityHandler.getSyncedFriendlyEntities(level))
            .filter { it.vehicle == null }  // 跳过乘客
            .distinctBy { it.id }

        for (e in all) {

            val entity = level.getEntity(e.id) ?: e
            val dx = entity.x - viewBlockX
            val dz = entity.z - viewBlockZ
            val screenX = mapCenterX + (dx * scale).toFloat()
            val screenY = mapCenterY + (dz * scale).toFloat()
            val icon = getVehicleIcon(entity)
            val iconSize = 12

            val clampedX = screenX.coerceIn((mapLeft + 4).toFloat(), (mapLeft + mapAreaW - 4).toFloat())
            val clampedY = screenY.coerceIn((mapTop + 4).toFloat(), (mapTop + mapAreaH - 4).toFloat())
            val alpha = if (screenX == clampedX && screenY == clampedY) 1f else 0.5f

            RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
            val pose = guiGraphics.pose()
            pose.pushPose()
            pose.translate(clampedX, clampedY, 0f)
            if (entity is VehicleEntity || entity is MissileProjectile) {
                pose.rotateAround(Axis.ZP.rotationDegrees(entity.yRot + 180f), 0f, 0f, 0f)
            }
            guiGraphics.blit(
                icon,
                -iconSize / 2, -iconSize / 2,
                0f, 0f, iconSize, iconSize, iconSize, iconSize
            )
            pose.popPose()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }
    }

    private fun getVehicleIcon(entity: Entity): net.minecraft.resources.ResourceLocation {
        return if (entity is Boat) {
            ICON_BOAT
        } else if (entity is VehicleEntity) {
            when (entity.vehicleType) {
                VehicleType.AIRPLANE -> ICON_AIRCRAFT
                VehicleType.HELICOPTER -> ICON_HELICOPTER
                VehicleType.APC -> ICON_APC
                VehicleType.CAR -> ICON_CAR
                VehicleType.AA -> ICON_AA
                VehicleType.TANK -> ICON_TANK
                VehicleType.ARTILLERY -> ICON_ARTILLERY
                VehicleType.DRONE -> ICON_DRONE
                VehicleType.BOAT -> ICON_BOAT
                VehicleType.DEFENSE -> ICON_DEFENSE
                VehicleType.AIRSHIP -> ICON_AIRSHIP
                else -> TEAMMATE_MARKER
            }
        } else if (entity.type.`is`(ModTags.EntityTypes.MINE)) {
            ICON_MINE
        } else if (entity is MissileProjectile) {
            ICON_MISSILE
        } else if (entity.type.descriptionId == "entity.touhou_little_maid.maid") {
            ICON_MAID
        } else {
            TEAMMATE_MARKER
        }
    }

    private fun renderPlayerMarker(guiGraphics: GuiGraphics, player: Player) {
        // 玩家坐 VehicleEntity 时只需要载具图标，不渲染玩家标记
        if (player.vehicle is VehicleEntity) return

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

    private fun renderCompassRose(guiGraphics: GuiGraphics) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f)
        guiGraphics.blit(COMPASS_ROSE, panelX + 6, panelY + 8, 0f, 0f, 32, 32, 32, 32)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    private fun renderHudText(guiGraphics: GuiGraphics, player: Player, mouseX: Int, mouseY: Int) {
        val font = minecraft!!.font
        val scale = zoom / 5.0

        // Coordinate text above the center button, left-aligned
        val posText = (if (mouseX in mapLeft..mapLeft + mapAreaW && mouseY in mapTop..mapTop + mapAreaH) {
            val wx = (viewBlockX + (mouseX - mapCenterX) / scale).toInt()
            val wz = (viewBlockZ + (mouseY - mapCenterY) / scale).toInt()
            val level = player.level()
            val chunk = level.getChunk(wx shr 4, wz shr 4)
            val chunkLoaded = chunk is LevelChunk && !chunk.isEmpty
            val wy = when {
                chunkLoaded -> {
                    val h = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        wx,
                        wz
                    ) - 1
                    if (h > level.minBuildHeight) h.toString() else "---"
                }

                else -> TacticalMapCache.getCachedHeight(wx, wz)?.let { (it - 1).toString() } ?: "---"
            }
            "$wx, $wy, $wz"
        } else {
            Component.translatable("hud.superbwarfare.tactical_map.unknown").string
        }) + "  ×${"%.1f".format(zoom)}"
        // Position text above the center button, left-aligned with it
        val textX = mapLeft
        val textY = mapTop + mapAreaH + 3
        guiGraphics.drawString(
            font,
            Component.translatable("hud.superbwarfare.tactical_map.pos", posText).string,
            textX, textY, 0xCC000000.toInt(), false
        )
    }

    // ========================
    //  Mouse input (pan + zoom + markers)
    // ========================

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val font = minecraft!!.font

        if (contextMenu.editPanelVisible) {
            if (pButton == 0) {
                if (contextMenu.editBoxMouseClicked(pMouseX, pMouseY, pButton)) return true
                if (contextMenu.handleEditPanelClick(pMouseX, pMouseY)) return true
            }
            return true
        }

        if (contextMenu.ctxMenuVisible) {
            if (contextMenu.handleContextMenuClick(pMouseX, pMouseY, font, width, height)) return true
            contextMenu.closeMenu()
            return true
        }

        // ── Connection mode ──
        if (connectionMode) {
            // Right-click in connection mode → exit
            if (pButton == 1) {
                connectionMode = false
                connectingFrom = null
                return true
            }
            // Left-click another marker → connect, then set as new source for chaining
            if (pButton == 0 && isMouseInPanel(pMouseX, pMouseY)) {
                val scale = zoom / 5.0
                val hit = contextMenu.hitTestMarker(
                    markers,
                    pMouseX,
                    pMouseY,
                    viewBlockX,
                    viewBlockZ,
                    scale,
                    mapCenterX,
                    mapCenterY
                )
                if (hit != null && hit.id != connectingFrom?.id) {
                    val src = connectingFrom!!
                    // Skip if already connected
                    val srcConns = connections[src.id]
                    if (srcConns == null || !srcConns.contains(hit.id)) {
                        connections.getOrPut(src.id) { mutableSetOf() }.add(hit.id)
                        connections.getOrPut(hit.id) { mutableSetOf() }.add(src.id)
                        saveMarker(src)
                        saveMarker(hit)
                    }
                    // Chain: target becomes new source
                    connectingFrom = hit
                    return true
                }
                return true
            }
            return true
        }

        // ── Line context menu click (only within button bounds) ──
        if (ctxLinePair != null) {
            val label = Component.translatable("context.superbwarfare.tactical_map.disconnect").string
            val pw = font.width(label) + 8
            val ph = 14
            if (pMouseX in ctxLineMenuX.toDouble()..(ctxLineMenuX + pw).toDouble() &&
                pMouseY in ctxLineMenuY.toDouble()..(ctxLineMenuY + ph).toDouble()
            ) {
                val (la, lb) = ctxLinePair!!
                connections[la.id]?.remove(lb.id)
                connections[lb.id]?.remove(la.id)
                saveMarker(la)
                saveMarker(lb)
                ctxLinePair = null
                ctxLineMenuX = 0
                ctxLineMenuY = 0
                return true
            }
            ctxLinePair = null
            ctxLineMenuX = 0
            ctxLineMenuY = 0
            return true
        }

        if (pButton == 0 && isMouseInPanel(pMouseX, pMouseY)) {
            val scale = zoom / 5.0
            val hit = contextMenu.hitTestMarker(
                markers,
                pMouseX,
                pMouseY,
                viewBlockX,
                viewBlockZ,
                scale,
                mapCenterX,
                mapCenterY
            )
            if (hit != null) {
                draggingMarker = hit
                dragOffsetX = (mapCenterX + (hit.x - viewBlockX) * scale) - pMouseX
                dragOffsetY = (mapCenterY + (hit.z - viewBlockZ) * scale) - pMouseY
                return true
            }
            isDragging = true
            lastMouseX = pMouseX
            lastMouseY = pMouseY
            return true
        }

        if (pButton == 1 && isMouseInPanel(pMouseX, pMouseY)) {
            val scale = zoom / 5.0
            val wX = (viewBlockX + (pMouseX - mapCenterX) / scale).toInt()
            val wZ = (viewBlockZ + (pMouseY - mapCenterY) / scale).toInt()

            // Marker hit-test takes priority over lines
            val hit = contextMenu.hitTestMarker(
                markers,
                pMouseX,
                pMouseY,
                viewBlockX,
                viewBlockZ,
                scale,
                mapCenterX,
                mapCenterY
            )
            if (hit != null) {
                contextMenu.openMarkerMenu(pMouseX.toInt(), pMouseY.toInt(), hit)
                return true
            }

            // Check line hit → freeze menu position at click
            for ((a, b) in getValidConnections()) {
                val ax = mapCenterX + (a.x - viewBlockX) * scale
                val ay = mapCenterY + (a.z - viewBlockZ) * scale
                val bx = mapCenterX + (b.x - viewBlockX) * scale
                val by = mapCenterY + (b.z - viewBlockZ) * scale
                if (hitTestLine(pMouseX, pMouseY, ax, ay, bx, by)) {
                    ctxLinePair = a to b
                    ctxLineMenuX = pMouseX.toInt() + 8
                    ctxLineMenuY = pMouseY.toInt()
                    return true
                }
            }
            val level = minecraft!!.player!!.level()
            val chunk = level.getChunk(wX shr 4, wZ shr 4)
            val chunkLoaded = chunk is LevelChunk && !chunk.isEmpty
            val wY = if (chunkLoaded) {
                level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, wX, wZ)
            } else {
                TacticalMapCache.getCachedHeight(wX, wZ)?.toInt() ?: minecraft!!.player!!.blockY
            }
            contextMenu.openMapMenu(pMouseX.toInt(), pMouseY.toInt(), wX, wY, wZ)
            return true
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        if (draggingMarker != null && pButton == 0) {
            val scale = zoom / 5.0
            val marker = draggingMarker!!
            val sx = pMouseX + dragOffsetX
            val sy = pMouseY + dragOffsetY
            marker.x = (viewBlockX + (sx - mapCenterX) / scale).toInt()
            marker.z = (viewBlockZ + (sy - mapCenterY) / scale).toInt()
            // 实时更新 Y 为地表高度；未绘制区域保持上一个有效值
            val h = TacticalMapCache.getCachedHeight(marker.x, marker.z)
            if (h != null) marker.y = h.toInt()
            return true
        }
        if (isDragging && pButton == 0) {
            followPlayer = false
            savedFollowPlayer = false
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
            draggingMarker?.let { saveMarker(it) }
            draggingMarker = null
            return true
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton)
    }

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, pScroll: Double): Boolean {
        if (isMouseInPanel(pMouseX, pMouseY)) {
            val oldScale = zoom / 5.0
            zoom = (zoom * (1.0 + pScroll * 0.15)).coerceIn(0.05, 20.0)
            val newScale = zoom / 5.0
            if (!followPlayer) {
                // Adjust view center so the world point under the mouse stays fixed
                viewBlockX += (pMouseX - mapCenterX) * (1.0 / oldScale - 1.0 / newScale)
                viewBlockZ += (pMouseY - mapCenterY) * (1.0 / oldScale - 1.0 / newScale)
            }
            DisplayConfig.TACTICAL_MAP_ZOOM.set(zoom)
            return true
        }
        return super.mouseScrolled(pMouseX, pMouseY, pScroll)
    }

    private fun isMouseInPanel(mx: Double, my: Double): Boolean {
        return mx >= mapLeft && mx <= mapLeft + mapAreaW && my >= mapTop && my <= mapTop + mapAreaH
    }

    override fun keyPressed(pKeyCode: Int, pScanCode: Int, pModifiers: Int): Boolean {
        if (contextMenu.editPanelVisible) {
            if (contextMenu.editBoxKeyPressed(pKeyCode, pScanCode, pModifiers)) return true
            if (pKeyCode == 256) {
                contextMenu.closeEditPanel()
                return true
            }
            return true
        }
        // Delete key (261): disconnect hovered line, or delete hovered marker
        if (pKeyCode == 261) {
            if (hoveredLine != null) {
                val (la, lb) = hoveredLine!!
                connections[la.id]?.remove(lb.id)
                connections[lb.id]?.remove(la.id)
                saveMarker(la)
                saveMarker(lb)
                hoveredLine = null
                return true
            }
            if (hoveredMarker != null && draggingMarker == null && !contextMenu.ctxMenuVisible) {
                val m = hoveredMarker!!
                val myConns = connections[m.id] ?: emptySet()
                for (otherId in myConns) {
                    val otherConns = connections[otherId]
                    if (otherConns != null && otherConns.remove(m.id)) {
                        val other = markers.find { it.id == otherId }
                        if (other != null) saveMarker(other)
                    }
                }
                connections.remove(m.id)
                markers.remove(m)
                deleteMarkerFile(m)
                hoveredMarker = null
                return true
            }
        }
        if (pKeyCode == ModKeyMappings.TOGGLE_TACTICAL_MAP.key.value || pKeyCode == 256) {
            onClose()
            return true
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers)
    }

    override fun charTyped(pCodePoint: Char, pModifiers: Int): Boolean {
        if (contextMenu.editPanelVisible) return contextMenu.editBoxCharTyped(pCodePoint, pModifiers)
        return super.charTyped(pCodePoint, pModifiers)
    }

    override fun renderBackground(pGuiGraphics: GuiGraphics) {
    }
}
