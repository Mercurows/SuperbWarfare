package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.map.TacticalMapCache
import com.atsuishio.superbwarfare.client.map.context.MapContextMenu
import com.atsuishio.superbwarfare.client.map.context.MapMarker
import com.atsuishio.superbwarfare.client.map.context.MarkerPersistence
import com.atsuishio.superbwarfare.client.screens.map.AttackModeHandler
import com.atsuishio.superbwarfare.client.screens.map.CoordinateConverter.scaleFromZoom
import com.atsuishio.superbwarfare.client.screens.map.MapEntityRenderer
import com.atsuishio.superbwarfare.client.screens.map.MissileWeaponHelper
import com.atsuishio.superbwarfare.client.screens.map.SelectionBoxManager
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.network.message.send.EntityAreaClearMessage
import com.atsuishio.superbwarfare.network.message.send.EntityClearMessage
import com.atsuishio.superbwarfare.network.message.send.VehicleFireMessage
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.sendPacketToServer
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
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.*
import kotlin.math.atan2

@OnlyIn(Dist.CLIENT)
class TacticalMapScreen : Screen(Component.translatable("container.superbwarfare.tactical_map")) {

    companion object {
        private var savedFollowPlayer = false

        // Persistent area selection boxes (survive GUI close, reset on game restart)
        data class SelBox(
            val id: Long,
            val worldMinX: Double, val worldMinZ: Double,
            val worldMaxX: Double, val worldMaxZ: Double,
        )
        val selBoxes = mutableListOf<SelBox>()
        private var selBoxIdCounter = 0L
        fun nextSelBoxId() = selBoxIdCounter++

        // Textures
        private val COMPASS_ROSE = loc("textures/overlay/tactical_map/compass_rose.png")
        private val PLAYER_MARKER = loc("textures/overlay/tactical_map/player_marker.png")
        private val TEAMMATE_MARKER = loc("textures/overlay/tactical_map/vehicle/indicator.png")
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
        private val RADAR_ICON = loc("textures/overlay/tactical_map/radar.png")
        private val PLAYER_DIRECTION = loc("textures/overlay/tactical_map/player_direction.png")

        // Attack mode
        private val ATTACK_CURSOR = loc("textures/overlay/tactical_map/attack.png")
        private val TARGET_FRAME = loc("textures/overlay/tactical_map/target_frame.png")
        private val TARGET_POS = loc("textures/overlay/tactical_map/target_pos.png")
        private val CRUISE_MARKER = loc("textures/overlay/tactical_map/cruise_marker.png")
        private val SEL_TARGET = loc("textures/overlay/tactical_map/sel_target.png")
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
    private var draggingLoiterPoint = false
    private var loiterDragOffX = 0.0
    private var loiterDragOffY = 0.0
    private var loiterDragNewX = 0.0
    private var loiterDragNewZ = 0.0
    private var loiterDragExpireTime = 0L
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0

    // Connection mode
    private var connectionMode = false
    private var connectingFrom: MapMarker? = null

    // ── Missile attack state (delegated to AttackModeHandler) ──

    // Line context menu
    private var ctxLinePair: Pair<MapMarker, MapMarker>? = null
    private var ctxLineMenuX = 0
    private var ctxLineMenuY = 0

    // Hovered line for highlight hovered marker for Delete key
    private var hoveredLine: Pair<MapMarker, MapMarker>? = null
    private var hoveredMarker: MapMarker? = null
    private var hoveredLoiterPoint = false
    private var hoveredSelBox: SelBox? = null

    // Entity hover tooltip
    private var hoveredEntityLines: List<Component>? = null
    private var hoveredEntityTipX = 0
    private var hoveredEntityTipY = 0

    // Entity right-click menu
    private val entityRenderList = mutableListOf<MapEntityRenderer.EntityRenderEntry>()
    private var entityMenuVisible = false
    private var entityMenuTarget: Entity? = null
    private var entityMenuX = 0
    private var entityMenuY = 0
    private val selectedEntities = mutableListOf<Entity>()

    // Area selection (shift + left-drag)
    private var selectionDragging = false
    private var selDragStartX = 0f
    private var selDragStartY = 0f
    private var selDragEndX = 0f
    private var selDragEndY = 0f
    // Selection right-click menu
    private var selMenuVisible = false
    private var selMenuTargetBox: SelBox? = null
    private var selMenuX = 0
    private var selMenuY = 0
    private var selMenuConfirmClear = false

    // Render delegates
    private val entityRenderer = MapEntityRenderer()
    private val attackHandler = AttackModeHandler()

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
        contextMenu.onLoiterPointEdit = {
            val vehicle = localPlayer?.vehicle as? VehicleEntity
            if (vehicle != null) {
                minecraft!!.setScreen(com.atsuishio.superbwarfare.client.screens.LoiterConfigScreen(vehicle))
            }
        }
        contextMenu.onLoiterPointDelete = {
            val vehicle = localPlayer?.vehicle as? VehicleEntity
            if (vehicle != null) {
                sendPacketToServer(
                    com.atsuishio.superbwarfare.network.message.send.LoiterConfigMessage(
                        centerX = vehicle.loiterCenterX.toFloat(),
                        centerY = vehicle.loiterCenterY.toFloat(),
                        centerZ = vehicle.loiterCenterZ.toFloat(),
                        radius = vehicle.loiterRadius.toFloat(),
                        active = false,
                        skipTerrain = false
                    )
                )
            }
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

        // Wire attack handler callbacks
        attackHandler.onFireMissile = { wx, wy, wz, name -> fireMissileAt(wx, wy, wz) }
        attackHandler.onGetAmmo = { name ->
            MissileWeaponHelper.currentAttackAmmo(name, getSelectedVehicles(), localPlayer)
        }
    }

    // ── Marker persistence: delegated to MarkerPersistence ──

    private fun loadMarkers() {
        MarkerPersistence.loadMarkers(minecraft!!, markers, connections)
    }

    private fun saveMarker(marker: MapMarker) {
        MarkerPersistence.saveMarker(minecraft!!, marker, connections)
    }

    private fun deleteMarkerFile(marker: MapMarker) {
        MarkerPersistence.deleteMarkerFile(minecraft!!, marker)
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
        TacticalMapCache.processPendingChunks(viewBlockX, viewBlockZ, 64)

        TacticalMapCache.processChunkUpdates(level, viewBlockX, viewBlockZ)

        // Periodic rescan: re-sample nearby chunks to catch block changes
        TacticalMapCache.periodicRescan(level, viewBlockX, viewBlockZ)

        // Upload dirty tile textures every tick so center-out loading is visible
        TacticalMapCache.uploadDirtyTextures()

        if (!markersLoaded) {
            loadMarkers()
            markersLoaded = true
        }

        // Sync direct attack ammo from actual GunData (tracks reloads)
        if (attackHandler.mode == AttackModeHandler.Mode.DIRECT && attackHandler.weaponName != null) {
            attackHandler.directAmmo = currentAttackAmmo()
        }

        // Refresh missile weapon ammo counts in open Level 2 menu (tracks reloads)
        if (contextMenu.missileSubMenuVisible && contextMenu.missileWeapons.isNotEmpty()) {
            val vehicles = getSelectedVehicles()
            if (vehicles.isNotEmpty()) {
                contextMenu.missileWeapons = contextMenu.missileWeapons.map { entry ->
                    // 汇总所有载具中该武器的弹药
                    val totalAmmo = vehicles.sumOf { v ->
                        val gd = v.gunDataMap[entry.weaponName] ?: return@sumOf 0
                        val ammoCost = gd.get(GunProp.AMMO_COST_PER_SHOOT)
                        if (ammoCost <= 0) 999 else gd.currentAvailableAmmo(localPlayer) / ammoCost
                    }
                    // Recompute display name so inline %1$s ammo placeholder stays in sync
                    val rawName = vehicles.firstNotNullOfOrNull { v ->
                        v.gunDataMap[entry.weaponName]?.get(GunProp.NAME) as? String
                    } ?: entry.weaponName
                    val translated = try {
                        Component.translatable(rawName).string
                    } catch (_: Exception) { rawName }
                    val ammoStr = "×$totalAmmo"
                    val newDisplay = if (translated.contains("%1\$s"))
                        translated.replace("%1\$s", ammoStr) else translated
                    entry.copy(ammoCount = totalAmmo, displayName = newDisplay)
                }
            }
        }

        // Sequential fire timer and ammo sync — delegated to AttackModeHandler
        attackHandler.tick()

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

        // Dark blue background (tiles cover explored areas)
        pGuiGraphics.fill(mapLeft, mapTop, mapLeft + mapAreaW, mapTop + mapAreaH, 0xFF110E25.toInt())

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

        // Layer 2.5: Area selection box (behind entities)
        renderSelectionBox(pGuiGraphics)

        // Layer 3: Friendly entity markers
        renderFriendlyMarkers(pGuiGraphics, player, pPartialTick, pMouseX, pMouseY)

        // Layer 3.5: Connection lines + preview in connection mode
        renderConnectionLines(pGuiGraphics, pMouseX, pMouseY)
        if (connectionMode && connectingFrom != null && isMouseInPanel(pMouseX.toDouble(), pMouseY.toDouble())) {
            renderConnectionPreview(pGuiGraphics, pMouseX, pMouseY)
        }

        // Layer 3.6: Position markers
        renderPositionMarkers(pGuiGraphics)

        // Layer 3.7: Queue attack targets
        if (attackHandler.mode == AttackModeHandler.Mode.QUEUE) {
            renderQueueTargets(pGuiGraphics, player)
        }

        // Layer 4: Player marker
        renderPlayerMarker(pGuiGraphics, player)

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        // === END CLIP ===
        pGuiGraphics.disableScissor()

        // Entity hover tooltip (rendered outside scissor to avoid clipping)
        val tipLines = hoveredEntityLines
        if (tipLines != null) {
            pGuiGraphics.renderTooltip(font, tipLines, Optional.empty(), hoveredEntityTipX, hoveredEntityTipY)
        }

        // Player direction indicator (arrow at map edge when player is off-screen)
        renderPlayerOffscreenIndicator(pGuiGraphics, player)

        // Border frame around map area
        renderMapBorder(pGuiGraphics)

        // Outside clip: Compass rose and HUD
        renderCompassRose(pGuiGraphics)
        renderHudText(pGuiGraphics, player, pMouseX, pMouseY)

        // ── Attack mode overlay (cursor, queue menu) ──
        if (attackHandler.mode == AttackModeHandler.Mode.DIRECT) {
            renderAttackCursor(pGuiGraphics, pMouseX, pMouseY)
        }

        // Queue context menu
        if (attackHandler.queueMenuVisible) {
            renderQueueMenu(pGuiGraphics, pMouseX, pMouseY)
        }

        // Marker hover tooltip (coordinates only, also shown while dragging)
        if (!contextMenu.ctxMenuVisible && !contextMenu.editPanelVisible) {
            val s = scaleFromZoom(zoom)
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
                hoveredLoiterPoint = hitTestLoiterPoint(pMouseX.toDouble(), pMouseY.toDouble())
                hoveredSelBox = hitTestSelectionBox(pMouseX.toDouble(), pMouseY.toDouble())
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

        // Entity right-click menu
        if (entityMenuVisible && entityMenuTarget != null) {
            renderEntityContextMenu(pGuiGraphics, font, pMouseX, pMouseY)
        }

        // Selection box right-click menu
        if (selMenuVisible) {
            renderSelContextMenu(pGuiGraphics, font, pMouseX, pMouseY)
        }

        contextMenu.render(pGuiGraphics, font, pMouseX, pMouseY, width, height)

        // Update follow button visual state (only when changed to avoid input glitches)
        if (followPlayer != lastFollowState) {
            lastFollowState = followPlayer
            followBtn.message = if (followPlayer) followIconActive else followIconNormal
        }

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)

        // Clear focus from bottom buttons after rendering to prevent
        // the white focus border from sticking after a click.
        centerBtn.setFocused(false)
        followBtn.setFocused(false)

        // Button tooltips (must render after buttons)
        val font = minecraft!!.font
        if (centerBtn.isMouseOver(pMouseX.toDouble(), pMouseY.toDouble())) {
            pGuiGraphics.renderTooltip(
                font,
                listOf(Component.translatable("context.superbwarfare.tactical_map.center_tooltip")),
                Optional.empty(),
                pMouseX, pMouseY
            )
        }
        if (followBtn.isMouseOver(pMouseX.toDouble(), pMouseY.toDouble())) {
            pGuiGraphics.renderTooltip(
                font,
                listOf(Component.translatable("context.superbwarfare.tactical_map.follow_tooltip")),
                Optional.empty(),
                pMouseX, pMouseY
            )
        }
    }

    private fun renderPositionMarkers(guiGraphics: GuiGraphics) {
        val scale = scaleFromZoom(zoom)
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
        val scale = scaleFromZoom(zoom)
        val font = minecraft!!.font
        val color = 0xFFFFFFFF.toInt()

        hoveredLine = null
        val clipL = mapLeft.toFloat(); val clipR = (mapLeft + mapAreaW).toFloat()
        val clipT = mapTop.toFloat(); val clipB = (mapTop + mapAreaH).toFloat()
        for ((a, b) in getValidConnections()) {
            val ax = (mapCenterX + (a.x - viewBlockX) * scale).toFloat()
            val ay = (mapCenterY + (a.z - viewBlockZ) * scale).toFloat()
            val bx = (mapCenterX + (b.x - viewBlockX) * scale).toFloat()
            val by = (mapCenterY + (b.z - viewBlockZ) * scale).toFloat()
            // 快速剔除：两端点都在可视区域外且线段不与区域相交
            if ((ax < clipL && bx < clipL) || (ax > clipR && bx > clipR) ||
                (ay < clipT && by < clipT) || (ay > clipB && by > clipB)) continue

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
        val scale = scaleFromZoom(zoom)
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

        // Dashed line via PoseStack rotation: 1px segments with 1px gaps, clipped to visible area
        val dashColor = 0xAAFFAA00.toInt()
        val range = MapEntityRenderer.clipDashRange(sx.toFloat(), sy.toFloat(), mx.toFloat(), my.toFloat(), mapLeft, mapTop, mapAreaW, mapAreaH)
        if (range != null) {
            val pose = guiGraphics.pose()
            pose.pushPose()
            pose.translate(midX, midY, 0f)
            pose.rotateAround(Axis.ZP.rotationDegrees(Math.toDegrees(angle).toFloat()), 0f, 0f, 0f)
            var x = range.first
            while (x < range.second) {
                guiGraphics.fill(x, 0, x + 1, 1, dashColor)
                x += 2
            }
            pose.popPose()
        }

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
        // Panel background
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF4F4F4F.toInt())
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

    private fun renderMapBorder(guiGraphics: GuiGraphics) {
        val borderColor = 0xFF000000.toInt() // green
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
        val scale = scaleFromZoom(zoom)
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
        val scale = scaleFromZoom(zoom)
        val gridBlockInterval = getGridInterval()
        val gridPixelInterval = (gridBlockInterval * scale).toFloat()
        if (gridPixelInterval < 8) return

        val originBlockX = (viewBlockX / gridBlockInterval).toInt() * gridBlockInterval
        val originBlockZ = (viewBlockZ / gridBlockInterval).toInt() * gridBlockInterval
        val lineColor = 0xCC00EE00.toInt()  // green — main grid
        val labelColor = 0xFFFFFFFF.toInt()  // white label with shadow
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
                        2, mapTop + 2, labelColor, true
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
                        mapLeft + 2, -10, labelColor, true
                    )
                pose.popPose()
            }
            blockZ += gridBlockInterval
        }

        // Chunk borderlines (every 16 blocks) — visible only at high zoom
        if (zoom > 15.0) {
            val chunkColor = 0x3300EE00.toInt()  // translucent green — chunk borders
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
        zoom >= 0.75 -> 250
        zoom >= 0.5 -> 500
        zoom >= 0.25 -> 1000
        zoom >= 0.15 -> 2500
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

    private fun renderFriendlyMarkers(guiGraphics: GuiGraphics, player: Player, pPartialTick: Float, mouseX: Int, mouseY: Int) {
        val scale = scaleFromZoom(zoom)
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

        // 雷达扇面置于最底层
        entityRenderer.renderRadars(level, scale, guiGraphics, mapCenterX, mapCenterY, viewBlockX, viewBlockZ)
        entityRenderList.clear()
        hoveredEntityLines = null

        val useDragPt = draggingLoiterPoint || System.currentTimeMillis() < loiterDragExpireTime

        // 友方（绿色）
        entityRenderer.renderEntityBatch(guiGraphics,
            ClientSyncedEntityHandler.getSyncedFriendlyEntities(level).filter { it.vehicle == null }.distinctBy { it.id },
            level, 0xFF7FFFAD.toInt(), "context.superbwarfare.tactical_map.relation.friendly",
            viewBlockX, viewBlockZ, mapCenterX, mapCenterY, mapLeft, mapTop, mapAreaW, mapAreaH,
            scale, pPartialTick, mouseX, mouseY, selectedEntities, entityRenderList,
            { lines, x, y -> hoveredEntityLines = lines; hoveredEntityTipX = x; hoveredEntityTipY = y },
            useDragPt, loiterDragNewX, loiterDragNewZ, loiterDragExpireTime
        )

        // 中立（灰色）
        entityRenderer.renderEntityBatch(guiGraphics,
            ClientSyncedEntityHandler.getSyncedNeutralEntities(level).filter { it.vehicle == null }.distinctBy { it.id },
            level, 0xFFAAAAAA.toInt(), "context.superbwarfare.tactical_map.relation.neutral",
            viewBlockX, viewBlockZ, mapCenterX, mapCenterY, mapLeft, mapTop, mapAreaW, mapAreaH,
            scale, pPartialTick, mouseX, mouseY, selectedEntities, entityRenderList,
            { lines, x, y -> hoveredEntityLines = lines; hoveredEntityTipX = x; hoveredEntityTipY = y },
            useDragPt, loiterDragNewX, loiterDragNewZ, loiterDragExpireTime
        )

        // 敌对（红色）
        entityRenderer.renderEntityBatch(guiGraphics,
            ClientSyncedEntityHandler.getSyncedHostileEntities(level).filter { it.vehicle == null }.distinctBy { it.id },
            level, 0xFFFF5555.toInt(), "context.superbwarfare.tactical_map.relation.hostile",
            viewBlockX, viewBlockZ, mapCenterX, mapCenterY, mapLeft, mapTop, mapAreaW, mapAreaH,
            scale, pPartialTick, mouseX, mouseY, selectedEntities, entityRenderList,
            { lines, x, y -> hoveredEntityLines = lines; hoveredEntityTipX = x; hoveredEntityTipY = y },
            useDragPt, loiterDragNewX, loiterDragNewZ, loiterDragExpireTime
        )

        // 实体已消失则关闭其右键菜单并取消选中
        if (entityMenuTarget != null && entityRenderList.none { it.entity === entityMenuTarget }) {
            entityMenuVisible = false
            entityMenuTarget = null
        }
        selectedEntities.removeAll { sel -> entityRenderList.none { it.entity.id == sel.id } }

        // 雷达图标置于最顶层
        entityRenderer.renderRadarsIcon(level, scale, guiGraphics, mapCenterX, mapCenterY, viewBlockX, viewBlockZ)

        RenderSystem.depthMask(true)
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * 在战术地图上渲染队友雷达图标和半透明绿色扇面。
     * 图层 0（最底层）—— 所有后续添加的图标都应渲染在此层之上。
     */

    private fun renderRadarsIcon(level: net.minecraft.world.level.Level, scale: Double, guiGraphics: GuiGraphics) {
        val radars = ClientSyncedEntityHandler.getSyncedRadars(level)
        if (radars.isEmpty()) return

        // 雷达图标
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        for (radar in radars) {
            if (radar.showIcon) {
                val rx = (mapCenterX + (radar.pos.x - viewBlockX) * scale).toFloat()
                val ry = (mapCenterY + (radar.pos.z - viewBlockZ) * scale).toFloat()
                val iconSize = 8

                RenderSystem.setShaderColor(1f, 1f, 1f, 0.9f)

                guiGraphics.blit(RADAR_ICON,
                    (rx - iconSize / 2).toInt(), (ry - iconSize / 2).toInt(),
                    0f, 0f, iconSize, iconSize, iconSize, iconSize)
            }

        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }
    private fun renderRadars(level: net.minecraft.world.level.Level, scale: Double, guiGraphics: GuiGraphics) {
        val radars = ClientSyncedEntityHandler.getSyncedRadars(level)
        if (radars.isEmpty()) return

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        // 绘制扇面（始终绘制）
        RenderSystem.setShader { GameRenderer.getPositionShader() }
        for (radar in radars) {
            val rx = (mapCenterX + (radar.pos.x - viewBlockX) * scale).toFloat()
            val ry = (mapCenterY + (radar.pos.z - viewBlockZ) * scale).toFloat()
            val pr = (radar.radius * scale).toFloat()
            val startAngle = (radar.yRot - radar.sweepAngle / 2.0 - 90.0).toFloat()
            val sweep = radar.sweepAngle.toFloat().coerceIn(0f, 360f)
            drawFilledSector(guiGraphics, rx, ry, pr, startAngle, sweep)
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /** 使用 POSITION shader + TRIANGLE_STRIP 绘制填充扇形（0~360° 即完整圆） */
    private fun drawFilledSector(guiGraphics: GuiGraphics, cx: Float, cy: Float, radius: Float, startDeg: Float, sweepDeg: Float) {
        if (radius < 2f || sweepDeg <= 0f) return
        RenderSystem.setShaderColor(0f, 1f, 0f, 0.2f)

        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(cx, cy, 0f)

        val tess = com.mojang.blaze3d.vertex.Tesselator.getInstance()
        val buf = tess.builder
        // 根据屏幕像素弧长动态计算分段数，小扇面少三角形以优化性能，允许棱角感
        val arcPixels = sweepDeg / 180f * kotlin.math.PI.toFloat() * radius
        val steps = (arcPixels / 30f).toInt().coerceIn(3, 12)
        buf.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLE_STRIP,
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION)
        val matrix = pose.last().pose()

        for (i in 0..steps) {
            val angleRad = Math.toRadians((startDeg + sweepDeg * i / steps).toDouble())
            val x = (kotlin.math.cos(angleRad) * radius).toFloat()
            val y = (kotlin.math.sin(angleRad) * radius).toFloat()
            buf.vertex(matrix, x, y, 0f).endVertex()
            buf.vertex(matrix, 0f, 0f, 0f).endVertex()
        }
        tess.end()
        pose.popPose()
    }

    /** Liang-Barsky 线裁剪：将屏幕空间线段裁剪到地图可视区域，返回本地虚线坐标范围 */
    private fun clipDashRange(sx: Float, sy: Float, ex: Float, ey: Float): Pair<Int, Int>? {
        val cx1 = mapLeft.toFloat()
        val cx2 = (mapLeft + mapAreaW).toFloat()
        val cy1 = mapTop.toFloat()
        val cy2 = (mapTop + mapAreaH).toFloat()

        val edgeP = floatArrayOf(-(ex - sx), ex - sx, -(ey - sy), ey - sy)
        val edgeQ = floatArrayOf(sx - cx1, cx2 - sx, sy - cy1, cy2 - sy)
        var tMin = 0f; var tMax = 1f

        for (i in 0 until 4) {
            if (edgeP[i] == 0f) { if (edgeQ[i] < 0) return null }
            else {
                val t = edgeQ[i] / edgeP[i]
                if (edgeP[i] < 0) tMin = maxOf(tMin, t) else tMax = minOf(tMax, t)
            }
        }
        if (tMin > tMax) return null
        val dx = ex - sx; val dy = ey - sy
        val len = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val half = (len / 2f).toInt()
        val lo = (-half + tMin * len).toInt()
        val hi = (-half + tMax * len).toInt()
        return maxOf(-half, lo) to minOf(half, hi)
    }

    private fun renderTargetPos(targetPos: Vec3, scale: Double, screenX: Float, screenY: Float, guiGraphics: GuiGraphics, entity: Entity) {
        val tdx = targetPos.x - viewBlockX
        val tdz = targetPos.z - viewBlockZ
        val targetScreenX = (mapCenterX + tdx * scale).toFloat()
        val targetScreenY = (mapCenterY + tdz * scale).toFloat()

        // 红色虚线（裁剪到地图可视区域）
        val ldx = targetScreenX - screenX
        val ldy = targetScreenY - screenY
        val len = kotlin.math.sqrt((ldx * ldx + ldy * ldy).toDouble()).toFloat()
        if (len > 2f) {
            val range = clipDashRange(screenX, screenY, targetScreenX, targetScreenY) ?: return
            val angle = atan2(ldy.toDouble(), ldx.toDouble())
            val midX = ((screenX + targetScreenX) / 2f)
            val midY = ((screenY + targetScreenY) / 2f)
            val dashColor = 0xAAFF0000.toInt()
            val linePose = guiGraphics.pose()
            linePose.pushPose()
            linePose.translate(midX, midY, 0f)
            linePose.rotateAround(Axis.ZP.rotationDegrees(Math.toDegrees(angle).toFloat()), 0f, 0f, 0f)
            var x = range.first
            while (x < range.second) {
                guiGraphics.fill(x, 0, x + 1, 1, dashColor)
                x += 2
            }
            linePose.popPose()

            // 距离标注
            val font = minecraft!!.font
            val dist = kotlin.math.sqrt((targetPos.x - entity.x) * (targetPos.x - entity.x) + (targetPos.z - entity.z) * (targetPos.z - entity.z))
            val label = "${dist.toInt()}m"
            guiGraphics.drawString(font, label, (targetScreenX + 10).toInt(), (targetScreenY + 6).toInt(), 0xFFFF0000.toInt(), true)
        }

        // 目标位置贴图 16x16，呼吸缩放 + 慢速顺时针旋转
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val time = System.currentTimeMillis() / 1000.0
        val breathScale = (1.0 + 0.25 * kotlin.math.sin(time * 4.0)).toFloat()  // ~1.5s 周期, 0.75~1.25
        val targetPose = guiGraphics.pose()
        targetPose.pushPose()
        targetPose.translate(targetScreenX, targetScreenY, 0f)
        targetPose.scale(breathScale, breathScale, 1f)
        guiGraphics.blit(TARGET_POS, -8, -8, 0f, 0f, 16, 16, 16, 16)
        targetPose.popPose()
    }

    /**
     * 在战术地图上渲染单个实体图标（含颜色染色、导弹目标位置、盘旋巡航点）。
     */
    private fun renderMapEntity(
        entity: Entity,
        level: net.minecraft.world.level.Level,
        scale: Double,
        pPartialTick: Float,
        guiGraphics: GuiGraphics,
        tintColor: Int
    ) {
        val r = ((tintColor shr 16) and 0xFF) / 255f
        val g = ((tintColor shr 8) and 0xFF) / 255f
        val b = (tintColor and 0xFF) / 255f

        val dx = entity.x - viewBlockX
        val dz = entity.z - viewBlockZ
        val screenX = mapCenterX + (dx * scale).toFloat()
        val screenY = mapCenterY + (dz * scale).toFloat()
        val icon = getVehicleIcon(entity)
        val iconSize = 12

        val clampedX = screenX.coerceIn((mapLeft + 4).toFloat(), (mapLeft + mapAreaW - 4).toFloat())
        val clampedY = screenY.coerceIn((mapTop + 4).toFloat(), (mapTop + mapAreaH - 4).toFloat())
        val alpha = if (screenX == clampedX && screenY == clampedY) 1f else 0.5f

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
        RenderSystem.setShaderColor(r, g, b, alpha)

        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(clampedX, clampedY, 0f)
        if (entity is VehicleEntity || entity is MissileProjectile) {
            pose.rotateAround(Axis.ZP.rotationDegrees(entity.yRot + 180f), 0f, 0f, 0f)
        }
        guiGraphics.blit(icon, -iconSize / 2, -iconSize / 2, 0f, 0f, iconSize, iconSize, iconSize, iconSize)
        pose.popPose()

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        // 渲染导弹目标位置
        if (entity is MissileProjectile) {
            val synced = ClientSyncedEntityHandler.getSyncedEntry(level, entity.id)
            val targetPos = entity.getTargetPos() ?: synced?.targetPos
            if (targetPos != null) {
                renderTargetPos(targetPos, scale, screenX, screenY, guiGraphics, entity)
            }
        }

        // 渲染自身飞机盘旋巡航点
        val player = localPlayer
        if (entity is VehicleEntity && entity.loiterActive && player != null && player.vehicle === entity) {
            val useDragPos = draggingLoiterPoint || System.currentTimeMillis() < loiterDragExpireTime
            val lx = if (useDragPos) loiterDragNewX else entity.loiterCenterX
            val lz = if (useDragPos) loiterDragNewZ else entity.loiterCenterZ
            val navScreenX = mapCenterX + (lx - viewBlockX) * scale
            val navScreenY = mapCenterY + (lz - viewBlockZ) * scale

            val ldx = navScreenX - screenX
            val ldy = navScreenY - screenY
            val len = kotlin.math.sqrt((ldx * ldx + ldy * ldy).toDouble()).toFloat()
            if (len > 2f) {
                val range = clipDashRange(screenX.toFloat(), screenY.toFloat(), navScreenX.toFloat(), navScreenY.toFloat())
                if (range != null) {
                val angle = atan2(ldy.toDouble(), ldx.toDouble())
                val midX = ((screenX + navScreenX) / 2f).toFloat()
                val midY = ((screenY + navScreenY) / 2f).toFloat()
                val dashColor = 0xAACDFFF6.toInt()
                val linePose = guiGraphics.pose()
                linePose.pushPose()
                linePose.translate(midX, midY, 0f)
                linePose.rotateAround(Axis.ZP.rotationDegrees(Math.toDegrees(angle).toFloat()), 0f, 0f, 0f)
                var ox = range.first
                while (ox < range.second) {
                    guiGraphics.fill(ox, 0, ox + 1, 1, dashColor)
                    ox += 2
                }
                linePose.popPose()
                val font = minecraft!!.font
                val dist = kotlin.math.sqrt((lx - entity.x) * (lx - entity.x) + (lz - entity.z) * (lz - entity.z))
                val label = "${dist.toInt()}m"
                guiGraphics.drawString(font, label, (navScreenX + 10).toInt(), (navScreenY + 6).toInt(), 0xFFCDFFF6.toInt(), true)
                }
            }

            val clampedNX = navScreenX.coerceIn(
                (mapLeft + 4).toDouble(), (mapLeft + mapAreaW - 4).toDouble()).toFloat()
            val clampedNY = navScreenY.coerceIn(
                (mapTop + 13).toDouble(), (mapTop + mapAreaH).toDouble()).toFloat()
            val navPose = guiGraphics.pose()
            navPose.pushPose()
            navPose.translate(clampedNX, clampedNY, 0f)
            guiGraphics.blit(CRUISE_MARKER, -4, -13, 0f, 0f, 8, 13, 8, 13)
            navPose.popPose()
        }
    }

    private fun drawSelectedBorder(guiGraphics: GuiGraphics, centerX: Float, centerY: Float) {
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
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        guiGraphics.blit(SEL_TARGET, (centerX - 8).toInt(), (centerY - 8).toInt(), 0f, 0f, 16, 16, 16, 16)
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
        val scale = scaleFromZoom(zoom)
        entityRenderer.renderPlayerMarker(guiGraphics, player, viewBlockX, viewBlockZ, mapCenterX, mapCenterY, scale)
    }

    /**
     * When the player is outside the visible map area, render a directional arrow at the
     * map edge pointing toward the player's location. 50% opacity.
     */
    private fun renderPlayerOffscreenIndicator(guiGraphics: GuiGraphics, player: Player) {
        val scale = scaleFromZoom(zoom)
        entityRenderer.renderPlayerOffscreenIndicator(guiGraphics, player, viewBlockX, viewBlockZ, mapCenterX, mapCenterY, scale, mapLeft, mapTop, mapAreaW, mapAreaH)
    }

    private fun renderEntityContextMenu(guiGraphics: GuiGraphics, font: net.minecraft.client.gui.Font, mouseX: Int, mouseY: Int) {
        val target = entityMenuTarget
        if (target == null || target.isRemoved) {
            entityMenuVisible = false
            entityMenuTarget = null
            return
        }
        val isFriendlyVehicle = target is VehicleEntity &&
            entityRenderList.any { it.entity === target && it.relation == "friendly" }
        val selectLabel = if (selectedEntities.any { it.id == target.id })
            Component.translatable("context.superbwarfare.tactical_map.entity_menu.deselect").string
        else
            Component.translatable("context.superbwarfare.tactical_map.entity_menu.select").string
        val teleportLabel = Component.translatable(
            "context.superbwarfare.tactical_map.entity_menu.teleport",
            target.x.toInt(), target.y.toInt() + 1, target.z.toInt()
        ).string
        val clearLabel = Component.translatable("context.superbwarfare.tactical_map.entity_menu.clear").string

        val isAdmin = minecraft?.player?.hasPermissions(2) ?: false
        val missileWeapons = buildEntityMissileWeapons(target)
        val itemHeight = 14
        val baseCount = 1 + (if (isFriendlyVehicle) 1 else 0) + (if (isAdmin) 1 else 0)
        val missileCount = missileWeapons.size
        val totalCount = baseCount + missileCount
        // Compute widest label
        var maxW = font.width(teleportLabel)
        if (isFriendlyVehicle) maxW = maxOf(maxW, font.width(selectLabel))
        if (isAdmin) maxW = maxOf(maxW, font.width(clearLabel))
        for (mw in missileWeapons) maxW = maxOf(maxW, font.width(mw.displayName))
        val menuW = maxW + 16
        val menuH = totalCount * itemHeight + 4 + (if (missileCount > 0) 1 else 0)

        var mx = entityMenuX + 8
        var my = entityMenuY
        if (mx + menuW > width) mx = entityMenuX - menuW - 8
        if (my + menuH > height) my = height - menuH

        // Background
        guiGraphics.fill(mx, my, mx + menuW, my + menuH, 0xEE2A2A2A.toInt())
        guiGraphics.fill(mx, my, mx + menuW, my + 1, 0xFF555555.toInt())
        guiGraphics.fill(mx, my + menuH - 1, mx + menuW, my + menuH, 0xFF555555.toInt())
        guiGraphics.fill(mx, my, mx + 1, my + menuH, 0xFF555555.toInt())
        guiGraphics.fill(mx + menuW - 1, my, mx + menuW, my + menuH, 0xFF555555.toInt())

        var idx = 0
        // Item 0: Teleport
        val ty0 = my + 2 + idx * itemHeight
        val hovered0 = mouseX in mx..mx + menuW && mouseY in ty0..ty0 + itemHeight
        if (hovered0) guiGraphics.fill(mx + 1, ty0, mx + menuW - 1, ty0 + itemHeight, 0x664444FF)
        guiGraphics.drawString(font, teleportLabel, mx + 8, ty0 + 3,
            if (hovered0) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt(), false)
        idx++

        // Item 1: Select / Deselect (friendly vehicle only)
        if (isFriendlyVehicle) {
            val ty1 = my + 2 + idx * itemHeight
            val hovered1 = mouseX in mx..mx + menuW && mouseY in ty1..ty1 + itemHeight
            if (hovered1) guiGraphics.fill(mx + 1, ty1, mx + menuW - 1, ty1 + itemHeight, 0x664444FF)
            guiGraphics.drawString(font, selectLabel, mx + 8, ty1 + 3,
                if (hovered1) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt(), false)
            idx++
        }

        // Item 2: Clear (admin only)
        if (isAdmin) {
            val ty2 = my + 2 + idx * itemHeight
            val hovered2 = mouseX in mx..mx + menuW && mouseY in ty2..ty2 + itemHeight
            if (hovered2) guiGraphics.fill(mx + 1, ty2, mx + menuW - 1, ty2 + itemHeight, 0x66444444)
            guiGraphics.drawString(font, clearLabel, mx + 8, ty2 + 3,
                if (hovered2) 0xFFFF5555.toInt() else 0xFFCC6666.toInt(), false)
            idx++
        }

        // Missile weapon items (separator line above first missile item)
        if (missileWeapons.isNotEmpty()) {
            val sepY = my + 2 + idx * itemHeight - 1
            guiGraphics.fill(mx + 4, sepY, mx + menuW - 4, sepY + 1, 0x44FFFFFF)
            for (mw in missileWeapons) {
                val ty = my + 2 + idx * itemHeight
                val hovered = mouseX in mx..mx + menuW && mouseY in ty..ty + itemHeight
                val disabled = mw.ammoCount <= 0
                val bg = if (hovered && !disabled) 0x66226644 else 0
                if (bg != 0) guiGraphics.fill(mx + 1, ty, mx + menuW - 1, ty + itemHeight, bg)
                val fg = when {
                    disabled -> 0xFF555555.toInt()
                    hovered -> 0xFFFFFFFF.toInt()
                    else -> 0xFFAACCAA.toInt()
                }
                guiGraphics.drawString(font, mw.displayName, mx + 8, ty + 3, fg, false)
                idx++
            }
        }
    }

    private fun handleEntityMenuClick(mouseX: Double, mouseY: Double): Boolean {
        val target = entityMenuTarget
        if (target == null || target.isRemoved) {
            entityMenuVisible = false
            entityMenuTarget = null
            return false
        }
        val isFriendlyVehicle = target is VehicleEntity &&
            entityRenderList.any { it.entity === target && it.relation == "friendly" }
        val selectLabel = if (selectedEntities.any { it.id == target.id })
            Component.translatable("context.superbwarfare.tactical_map.entity_menu.deselect").string
        else
            Component.translatable("context.superbwarfare.tactical_map.entity_menu.select").string
        val teleportLabel = Component.translatable(
            "context.superbwarfare.tactical_map.entity_menu.teleport",
            target.x.toInt(), target.y.toInt() + 1, target.z.toInt()
        ).string
        val clearLabel = Component.translatable("context.superbwarfare.tactical_map.entity_menu.clear").string

        val isAdmin = minecraft?.player?.hasPermissions(2) ?: false
        val missileWeapons = buildEntityMissileWeapons(target)
        val itemHeight = 14
        val baseCount = 1 + (if (isFriendlyVehicle) 1 else 0) + (if (isAdmin) 1 else 0)
        val missileCount = missileWeapons.size
        val totalCount = baseCount + missileCount
        val font = minecraft!!.font
        var maxW = font.width(teleportLabel)
        if (isFriendlyVehicle) maxW = maxOf(maxW, font.width(selectLabel))
        if (isAdmin) maxW = maxOf(maxW, font.width(clearLabel))
        for (mw in missileWeapons) maxW = maxOf(maxW, font.width(mw.displayName))
        val menuW = maxW + 16
        val menuH = totalCount * itemHeight + 4 + (if (missileCount > 0) 1 else 0)

        var mx = entityMenuX + 8
        var my = entityMenuY
        if (mx + menuW > width) mx = entityMenuX - menuW - 8
        if (my + menuH > height) my = height - menuH

        // Row 0: Teleport
        var ty = my + 2
        if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in ty.toDouble()..(ty + itemHeight).toDouble()) {
            minecraft?.player?.connection?.sendCommand("tp ${target.x.toInt()} ${target.y.toInt() + 1} ${target.z.toInt()}")
            entityMenuVisible = false
            entityMenuTarget = null
            return true
        }

        // Row 1: Select / Deselect (friendly vehicle only)
        if (isFriendlyVehicle) {
            ty += itemHeight
            if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in ty.toDouble()..(ty + itemHeight).toDouble()) {
                val idx = selectedEntities.indexOfFirst { it.id == target.id }
                if (idx >= 0) selectedEntities.removeAt(idx) else selectedEntities.add(target)
                entityMenuVisible = false
                entityMenuTarget = null
                return true
            }
        }

        // Row 2: Clear (admin)
        if (isAdmin) {
            ty += itemHeight
            if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in ty.toDouble()..(ty + itemHeight).toDouble()) {
                sendPacketToServer(EntityClearMessage(target.id))
                entityMenuVisible = false
                entityMenuTarget = null
                return true
            }
        }

        // Missile weapon rows
        if (missileWeapons.isNotEmpty()) {
            ty += itemHeight + 1 // skip separator line
            for (mw in missileWeapons) {
                if (mw.ammoCount <= 0) { ty += itemHeight; continue }
                if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in ty.toDouble()..(ty + itemHeight).toDouble()) {
                    fireEntityMissile(target, mw)
                    // Don't close menu — allow multiple shots
                    return true
                }
                ty += itemHeight
            }
        }

        return false
    }

    private data class EntityMissileWeapon(
        val weaponName: String,
        val displayName: String,
        val ammoCount: Int,
        val lockEntity: Boolean,  // true = lock UUID, false = use position
    )

    /** 获取所有被选中的载具（用于遥控打击），若未选中则回退到当前骑乘载具 */
    private fun getSelectedVehicles(): List<VehicleEntity> =
        MissileWeaponHelper.getSelectedVehicles(selectedEntities, localPlayer)

    /** 在所有选中载具中查找第一个拥有指定武器且有弹药的载具 */
    private fun findFirstVehicleWithWeapon(weaponName: String): VehicleEntity? =
        MissileWeaponHelper.findFirstVehicleWithWeapon(weaponName, getSelectedVehicles(), localPlayer)

    private fun buildEntityMissileWeapons(entity: Entity): List<EntityMissileWeapon> {
        val vehicles = getSelectedVehicles()
        return MissileWeaponHelper.aggregateWeapons(vehicles, entity, requireLockEntity = true, requireLockBlock = true)
            .map { EntityMissileWeapon(it.weaponName, it.displayNameBase, it.totalAmmo, it.canLockEntity) }
    }

    private fun fireEntityMissile(entity: Entity, weapon: EntityMissileWeapon) {
        // 遥控发射：在所有选中载具中找第一个有弹药的
        val shooter = findFirstVehicleWithWeapon(weapon.weaponName)
            ?: (localPlayer?.vehicle as? VehicleEntity)
            ?: return
        val remoteShooterId = if (shooter !== localPlayer?.vehicle) shooter.id else null
        val targetPos = SerializedVector3f(entity.x.toFloat(), (entity.y + 1.5).toFloat(), entity.z.toFloat())
        sendPacketToServer(VehicleFireMessage(
            uuid = if (weapon.lockEntity) entity.uuid else null,
            targetPos = targetPos,
            weaponName = weapon.weaponName,
            shooterVehicleId = remoteShooterId,
        ))
    }

    private fun renderSelectionBox(guiGraphics: GuiGraphics) {
        SelectionBoxManager.render(guiGraphics, selBoxes, viewBlockX, viewBlockZ, mapCenterX, mapCenterY, zoom,
            selectionDragging, selDragStartX, selDragStartY, selDragEndX, selDragEndY,
            minecraft!!.font, mapLeft, mapTop, mapAreaW, mapAreaH)
    }

    private fun hitTestSelectionBox(mouseX: Double, mouseY: Double): SelBox? {
        return SelectionBoxManager.hitTestBox(mouseX, mouseY, selBoxes, viewBlockX, viewBlockZ, mapCenterX, mapCenterY, zoom)
    }

    private fun renderSelContextMenu(guiGraphics: GuiGraphics, font: net.minecraft.client.gui.Font, mouseX: Int, mouseY: Int) {
        SelectionBoxManager.renderContextMenu(guiGraphics, font, mouseX, mouseY,
            selMenuX, selMenuY, width, height,
            minecraft?.player?.hasPermissions(2) ?: false, selMenuConfirmClear)
    }

    private fun handleSelMenuClick(mouseX: Double, mouseY: Double): Boolean {
        val targetBox = selMenuTargetBox ?: return false
        val isAdmin = minecraft?.player?.hasPermissions(2) ?: false
        val font = minecraft!!.font

        when (SelectionBoxManager.handleMenuClick(mouseX, mouseY, selMenuX, selMenuY, width, height, isAdmin, selMenuConfirmClear, font)) {
            1 -> {
                selBoxes.remove(targetBox)
                selMenuVisible = false
                selMenuConfirmClear = false
                selMenuTargetBox = null
                return true
            }
            2 -> {
                if (!selMenuConfirmClear) {
                    selMenuConfirmClear = true
                    return true
                }
                sendPacketToServer(EntityAreaClearMessage(
                    targetBox.worldMinX, -64.0, targetBox.worldMinZ,
                    targetBox.worldMaxX, 320.0, targetBox.worldMaxZ
                ))
                selMenuVisible = false
                selMenuTargetBox = null
                selMenuConfirmClear = false
                return true
            }
        }
        return false
    }

    private fun renderCompassRose(guiGraphics: GuiGraphics) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.6f)
        guiGraphics.blit(COMPASS_ROSE, panelX + 6, panelY + 8, 0f, 0f, 32, 32, 32, 32)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    private fun renderHudText(guiGraphics: GuiGraphics, player: Player, mouseX: Int, mouseY: Int) {
        val font = minecraft!!.font
        val scale = scaleFromZoom(zoom)

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
            Component.translatable("context.superbwarfare.tactical_map.unknown").string
        }) + "  ×${"%.1f".format(zoom)}"
        // Position text above the center button, left-aligned with it
        val textX = mapLeft
        val textY = mapTop + mapAreaH + 3
        val fullPosStr = Component.translatable("context.superbwarfare.tactical_map.pos", posText).string
        guiGraphics.drawString(font, fullPosStr, textX, textY, 0xFFFFFFFF.toInt(), false)

        // Attack mode hints — placed 2px after the position/zoom text
        val hintX = textX + font.width(fullPosStr) + 2
        if (attackHandler.mode == AttackModeHandler.Mode.DIRECT) {
            val hint = Component.translatable("context.superbwarfare.tactical_map.direct_attack_mode").string
            guiGraphics.drawString(font, hint, hintX, textY, 0xFFFFFFFF.toInt(), false)
        } else if (attackHandler.mode == AttackModeHandler.Mode.QUEUE) {
            val hint = Component.translatable("context.superbwarfare.tactical_map.queue_attack_mode").string
            guiGraphics.drawString(font, hint, hintX, textY, 0xFFFFFFFF.toInt(), false)
        } else if (connectionMode) {
            val hint = Component.translatable("context.superbwarfare.tactical_map.connect_mode").string
            guiGraphics.drawString(font, hint, hintX, textY, 0xFFFFFFFF.toInt(), false)
        }
    }

    // ========================
    //  Mouse input (pan + zoom + markers)
    // ========================

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        val font = minecraft!!.font

        // ── Queue context menu click ──
        if (attackHandler.queueMenuVisible) {
            return handleQueueMenuClick(pMouseX, pMouseY)
        }

        // ── Attack mode clicks (handled separately from normal map interaction) ──
        if (attackHandler.mode != AttackModeHandler.Mode.NONE) {
            return handleAttackModeClick(pMouseX, pMouseY, pButton)
        }

        // ── Selection box right-click ──
        val hitBox = if (pButton == 1) hitTestSelectionBox(pMouseX, pMouseY) else null
        if (hitBox != null) {
            selMenuTargetBox = hitBox
            selMenuVisible = true
            selMenuConfirmClear = false
            selMenuX = pMouseX.toInt()
            selMenuY = pMouseY.toInt()
            return true
        }

        // ── Entity right-click detection (before menu handling, allows switching targets) ──
        if (pButton == 1 && isMouseInPanel(pMouseX, pMouseY)) {
            for (entry in entityRenderList) {
                if (pMouseX.toFloat() in (entry.screenX - 6)..(entry.screenX + 6) &&
                    pMouseY.toFloat() in (entry.screenY - 6)..(entry.screenY + 6)
                ) {
                    entityMenuTarget = entry.entity
                    entityMenuX = pMouseX.toInt()
                    entityMenuY = pMouseY.toInt()
                    entityMenuVisible = true
                    return true
                }
            }
        }

        // ── Selection context menu clicks ──
        if (selMenuVisible) {
            if (handleSelMenuClick(pMouseX, pMouseY)) return true
            selMenuVisible = false
            selMenuConfirmClear = false
            return true
        }

        // ── Entity context menu clicks ──
        if (entityMenuVisible) {
            if (handleEntityMenuClick(pMouseX, pMouseY)) return true
            entityMenuVisible = false
            entityMenuTarget = null
            return true
        }

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

        if (contextMenu.loiterPointMenuVisible) {
            contextMenu.handleLoiterPointMenuClick(pMouseX.toInt(), pMouseY.toInt())
            contextMenu.closeLoiterPointMenu()
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
                val scale = scaleFromZoom(zoom)
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
            val scale = scaleFromZoom(zoom)
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
            // Loiter point hit-test → start dragging
            if (hitTestLoiterPoint(pMouseX, pMouseY)) {
                draggingLoiterPoint = true
                val vehicle = localPlayer?.vehicle as? VehicleEntity ?: return true
                loiterDragOffX = (mapCenterX + (vehicle.loiterCenterX - viewBlockX) * scale) - pMouseX
                loiterDragOffY = (mapCenterY + (vehicle.loiterCenterZ - viewBlockZ) * scale) - pMouseY
                return true
            }
            // Shift + left-click → area selection (instead of map panning)
            if (hasShiftDown()) {
                selMenuVisible = false
                selectionDragging = true
                selDragStartX = pMouseX.toFloat()
                selDragStartY = pMouseY.toFloat()
                selDragEndX = pMouseX.toFloat()
                selDragEndY = pMouseY.toFloat()
                return true
            }
            isDragging = true
            lastMouseX = pMouseX
            lastMouseY = pMouseY
            return true
        }

        if (pButton == 1 && isMouseInPanel(pMouseX, pMouseY)) {
            val scale = scaleFromZoom(zoom)
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

            // Loiter point right-click → show loiter point menu
            if (hitTestLoiterPoint(pMouseX, pMouseY)) {
                contextMenu.openLoiterPointMenu(pMouseX.toInt(), pMouseY.toInt())
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
            // ── Missile strike setup ──
            contextMenu.missileWeapons = emptyList()
            contextMenu.onMissileStrike = null
            val riddenVehicle = localPlayer?.vehicle as? VehicleEntity
            val sourceVehicles = getSelectedVehicles()
            if (sourceVehicles.isNotEmpty()) {
                val groundWeapons = MissileWeaponHelper.aggregateWeapons(
                    sourceVehicles, null, requireLockEntity = false, requireLockBlock = true
                )
                if (groundWeapons.isNotEmpty()) {
                    val weapons = groundWeapons.map {
                        MapContextMenu.MissileWeaponEntry(it.weaponName,
                            it.displayNameBase.replace("%1\$s", "×${it.totalAmmo}"), it.totalAmmo)
                    }
                    contextMenu.missileWeapons = weapons
                    contextMenu.onDirectAttack = { weaponName ->
                        attackHandler.enterDirectMode(weaponName, weapons.find { it.weaponName == weaponName }?.ammoCount ?: 0)
                    }
                    contextMenu.onQueueAttack = { weaponName ->
                        val firstVeh = sourceVehicles.firstOrNull { it.gunDataMap.containsKey(weaponName) }
                        attackHandler.enterQueueMode(weaponName, firstVeh)
                    }
                    contextMenu.onMissileStrike = null
                }
            }

            // ── Cruise here setup ──
            contextMenu.canCruiseHere = false
            contextMenu.onCruiseHere = null
            if (riddenVehicle != null && riddenVehicle.computed().engineType == EngineType.AIRCRAFT) {
                contextMenu.canCruiseHere = true
                contextMenu.onCruiseHere = { worldX, worldZ ->
                    val cachedH = TacticalMapCache.getCachedHeight(worldX, worldZ)
                    val (targetY, skipTerrain) = if (cachedH != null) {
                        (cachedH + 200).toFloat() to true
                    } else {
                        0f to false
                    }
                    sendPacketToServer(
                        com.atsuishio.superbwarfare.network.message.send.LoiterConfigMessage(
                            centerX = worldX.toFloat(),
                            centerY = targetY,
                            centerZ = worldZ.toFloat(),
                            radius = riddenVehicle.loiterRadius.toFloat(),
                            active = true,
                            skipTerrain = skipTerrain
                        )
                    )
                }
            }

            contextMenu.openMapMenu(pMouseX.toInt(), pMouseY.toInt(), wX, wY, wZ)
            return true
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun mouseDragged(pMouseX: Double, pMouseY: Double, pButton: Int, pDragX: Double, pDragY: Double): Boolean {
        // Update selection rectangle while dragging with shift
        if (selectionDragging && pButton == 0) {
            selDragEndX = pMouseX.toFloat()
            selDragEndY = pMouseY.toFloat()
            return true
        }
        if (draggingMarker != null && pButton == 0) {
            val scale = scaleFromZoom(zoom)
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
        if (draggingLoiterPoint && pButton == 0) {
            val scale = scaleFromZoom(zoom)
            val vehicle = localPlayer?.vehicle as? VehicleEntity ?: return true
            val sx = pMouseX + loiterDragOffX
            val sy = pMouseY + loiterDragOffY
            loiterDragNewX = (viewBlockX + (sx - mapCenterX) / scale)
            loiterDragNewZ = (viewBlockZ + (sy - mapCenterY) / scale)
            return true
        }
        if (isDragging && pButton == 0) {
            followPlayer = false
            savedFollowPlayer = false
            val scale = scaleFromZoom(zoom)
            viewBlockX -= (pMouseX - lastMouseX) / scale
            viewBlockZ -= (pMouseY - lastMouseY) / scale
            lastMouseX = pMouseX
            lastMouseY = pMouseY
            return true
        }
        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)
    }

    override fun mouseReleased(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        // Finalize area selection
        if (selectionDragging && pButton == 0) {
            selectionDragging = false
            val scale = scaleFromZoom(zoom)
            val minSX = minOf(selDragStartX, selDragEndX)
            val maxSX = maxOf(selDragStartX, selDragEndX)
            val minSY = minOf(selDragStartY, selDragEndY)
            val maxSY = maxOf(selDragStartY, selDragEndY)
            if ((maxSX - minSX) > 4f && (maxSY - minSY) > 4f) {
                val wMinX = viewBlockX + (minSX - mapCenterX) / scale
                val wMinZ = viewBlockZ + (minSY - mapCenterY) / scale
                val wMaxX = viewBlockX + (maxSX - mapCenterX) / scale
                val wMaxZ = viewBlockZ + (maxSY - mapCenterY) / scale
                selBoxes.add(SelBox(
                    nextSelBoxId(),
                    minOf(wMinX, wMaxX), minOf(wMinZ, wMaxZ),
                    maxOf(wMinX, wMaxX), maxOf(wMinZ, wMaxZ)
                ))
                // 快捷选取框选区域内的所有友方载具
                val boxMinX = minOf(wMinX, wMaxX)
                val boxMaxX = maxOf(wMinX, wMaxX)
                val boxMinZ = minOf(wMinZ, wMaxZ)
                val boxMaxZ = maxOf(wMinZ, wMaxZ)
                val level = minecraft?.player?.level() ?: return true
                for (e in ClientSyncedEntityHandler.getSyncedFriendlyEntities(level)) {
                    val entity = level.getEntity(e.id) ?: e
                    if (entity is VehicleEntity && entity.x in boxMinX..boxMaxX && entity.z in boxMinZ..boxMaxZ) {
                        if (selectedEntities.none { it.id == entity.id }) {
                            selectedEntities.add(entity)
                        }
                    }
                }
            }
            return true
        }
        if (pButton == 0) {
            isDragging = false
            draggingMarker?.let { saveMarker(it) }
            draggingMarker = null
            if (draggingLoiterPoint) {
                draggingLoiterPoint = false
                loiterDragExpireTime = System.currentTimeMillis() + 500
                val vehicle = localPlayer?.vehicle as? VehicleEntity
                if (vehicle != null) {
                    sendPacketToServer(
                        com.atsuishio.superbwarfare.network.message.send.LoiterConfigMessage(
                            centerX = loiterDragNewX.toFloat(),
                            centerY = vehicle.loiterCenterY.toFloat(),
                            centerZ = loiterDragNewZ.toFloat(),
                            radius = vehicle.loiterRadius.toFloat(),
                            active = true,
                            skipTerrain = false
                        )
                    )
                }
            }
            return true
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton)
    }

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, pScroll: Double): Boolean {
        if (isMouseInPanel(pMouseX, pMouseY)) {
            val oldScale = scaleFromZoom(zoom)
            zoom = (zoom * (1.0 + pScroll * 0.15)).coerceIn(0.05, 20.0)
            val newScale = scaleFromZoom(zoom)
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

    /** 检测鼠标是否点击了盘旋巡航点（底边中点锚定，8x13判定区域） */
    private fun hitTestLoiterPoint(mx: Double, my: Double): Boolean {
        val player = localPlayer ?: return false
        val vehicle = player.vehicle as? VehicleEntity ?: return false
        if (!vehicle.loiterActive || vehicle.computed().engineType != EngineType.AIRCRAFT) return false
        val scale = scaleFromZoom(zoom)
        val ax = mapCenterX + (vehicle.loiterCenterX - viewBlockX) * scale
        val ay = mapCenterY + (vehicle.loiterCenterZ - viewBlockZ) * scale
        return mx >= ax - 4 && mx <= ax + 4 && my >= ay - 13 && my <= ay
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
        // Delete key (261): disconnect hovered line, delete hovered selection box, or delete hovered marker
        if (pKeyCode == 261) {
            // Selection box: Delete removes hovered box
            val box = hoveredSelBox
            if (box != null) {
                selBoxes.remove(box)
                hoveredSelBox = null
                return true
            }
            // 巡航点优先：Delete 关闭盘旋
            if (hoveredLoiterPoint) {
                val vehicle = localPlayer?.vehicle as? VehicleEntity
                if (vehicle != null) {
                    sendPacketToServer(
                        com.atsuishio.superbwarfare.network.message.send.LoiterConfigMessage(
                            centerX = vehicle.loiterCenterX.toFloat(),
                            centerY = vehicle.loiterCenterY.toFloat(),
                            centerZ = vehicle.loiterCenterZ.toFloat(),
                            radius = vehicle.loiterRadius.toFloat(),
                            active = false,
                            skipTerrain = false
                        )
                    )
                }
                return true
            }
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

    // ═══════════════════════════════════════════════════════════════
    //  Missile attack mode rendering & logic
    // ═══════════════════════════════════════════════════════════════

    private fun renderAttackCursor(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        attackHandler.renderAttackCursor(guiGraphics, mouseX, mouseY, minecraft!!.font)
    }

    private fun renderQueueTargets(guiGraphics: GuiGraphics, player: Player) {
        attackHandler.renderQueueTargets(guiGraphics, viewBlockX, viewBlockZ, mapCenterX, mapCenterY, zoom, minecraft!!.font)
    }

    private fun renderQueueMenu(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        attackHandler.renderQueueMenu(guiGraphics, mouseX, mouseY, minecraft!!.font, width, height)
    }

    private fun fireMissileAt(worldX: Int, worldY: Int, worldZ: Int) {
        val name = attackHandler.weaponName ?: return
        // 遥控发射：在所有选中载具中找第一个有弹药的
        val shooter = findFirstVehicleWithWeapon(name)
            ?: (localPlayer?.vehicle as? VehicleEntity)
            ?: return
        val remoteShooterId = if (shooter !== localPlayer?.vehicle) shooter.id else null
        sendPacketToServer(
            VehicleFireMessage(
                uuid = null,
                targetPos = SerializedVector3f(worldX.toFloat(), worldY + 1.5f, worldZ.toFloat()),
                weaponName = name,
                shooterVehicleId = remoteShooterId,
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════
    //  Attack mode mouse handling (called from mouseClicked)
    // ═══════════════════════════════════════════════════════════════

    private fun handleAttackModeClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return attackHandler.handleClick(mouseX, mouseY, button,
            isMouseInPanel(mouseX, mouseY),
            viewBlockX, viewBlockZ, mapCenterX, mapCenterY, zoom,
            minecraft!!.player!!.level())
    }

    private fun handleQueueMenuClick(mouseX: Double, mouseY: Double): Boolean {
        return attackHandler.handleQueueMenuClick(mouseX, mouseY, minecraft!!.font, width, height)
    }

    /** 所有选中载具（或当前骑乘载具）中指定武器的总弹药数 */
    private fun currentAttackAmmo(): Int {
        val name = attackHandler.weaponName ?: return 0
        return MissileWeaponHelper.currentAttackAmmo(name, getSelectedVehicles(), localPlayer)
    }

    override fun renderBackground(pGuiGraphics: GuiGraphics) {
    }
}
