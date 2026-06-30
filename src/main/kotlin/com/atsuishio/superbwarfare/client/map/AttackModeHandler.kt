package com.atsuishio.superbwarfare.client.map

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen.Companion.SelBox
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 战术地图攻击模式控制器。
 * 管理 DIRECT / QUEUE 两种导弹攻击模式的状态、渲染和交互逻辑。
 *
 * 使用回调模式与 Screen 通信（参考 MapContextMenu 的委托模式）。
 */
class AttackModeHandler {

    companion object {
        val ATTACK_CURSOR = loc("textures/overlay/tactical_map/attack.png")
        val TARGET_FRAME = loc("textures/overlay/tactical_map/target_frame.png")
    }

    enum class Mode { NONE, DIRECT, QUEUE, BOMBARDMENT }

    // ── State (publicly readable use enter* / exitMode for mutations) ──
    var mode = Mode.NONE
    var weaponName: String? = null
    val targetQueue = mutableListOf<BlockPos>()
    var fireInterval = 0
    var directAmmo = 0
    var bombardmentAmmo = 0
    var maxGuidedRange = 2048.0
    var sourcePositions: List<Vec3> = emptyList()

    /** The selection box currently hovered by the mouse in BOMBARDMENT mode. Set by the Screen each render frame. */
    var hoveredBombardBox: SelBox? = null

    var queueMenuVisible = false
    var queueMenuX = 0
    var queueMenuY = 0

    private var seqFireActive = false
    private var seqFireTimer = 0
    private var seqFireIndex = 0

    // ── Callbacks (set by Screen) ──
    var onFireMissile: ((worldX: Int, worldY: Int, worldZ: Int, weaponName: String) -> Unit)? = null
    var onGetAmmo: ((weaponName: String) -> Int)? = null

    // ── Mode entry / exit ──

    fun enterDirectMode(name: String, ammo: Int) {
        mode = Mode.DIRECT
        weaponName = name
        directAmmo = ammo
    }

    fun enterQueueMode(name: String, firstVehicle: VehicleEntity?) {
        mode = Mode.QUEUE
        weaponName = name
        targetQueue.clear()
        fireInterval = firstVehicle?.gunDataMap?.get(name)?.get(GunProp.SHOOT_DELAY_TIME)?.coerceAtLeast(4) ?: 10
    }

    fun enterBombardmentMode(name: String, ammo: Int) {
        mode = Mode.BOMBARDMENT
        weaponName = name
        bombardmentAmmo = ammo
    }

    fun exitMode() {
        seqFireActive = false
        mode = Mode.NONE
        weaponName = null
        targetQueue.clear()
        seqFireTimer = 0
        seqFireIndex = 0
        queueMenuVisible = false
        bombardmentAmmo = 0
        hoveredBombardBox = null
    }

    // ── Tick ──

    /** 每 tick 同步弹药并驱动顺序射击。调用方需传入 level（用于高度查询，当前未使用但保留签名）。 */
    fun tick() {
        // Sync direct attack ammo
        if (mode == Mode.DIRECT && weaponName != null) {
            directAmmo = onGetAmmo?.invoke(weaponName!!) ?: 0
        }
        // Sync bombardment ammo
        if (mode == Mode.BOMBARDMENT && weaponName != null) {
            bombardmentAmmo = onGetAmmo?.invoke(weaponName!!) ?: 0
        }
        // Sequential fire
        if (seqFireActive) {
            if (seqFireIndex >= targetQueue.size) {
                seqFireActive = false
                return
            }
            if (seqFireTimer > 0) {
                seqFireTimer--
                return
            }
            if ((onGetAmmo?.invoke(weaponName ?: return) ?: 0) <= 0) return
            val pos = targetQueue[seqFireIndex]
            onFireMissile?.invoke(pos.x, pos.y, pos.z, weaponName!!)
            seqFireIndex++
            seqFireTimer = fireInterval
        }
    }

    // ── Rendering ──

    fun renderAttackCursor(
        guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, font: Font,
        viewBlockX: Double, viewBlockZ: Double, mapCenterX: Float, mapCenterY: Float, zoom: Double
    ) {
        // ── BOMBARDMENT mode: simple ammo counter at cursor, no crosshair ──
        if (mode == Mode.BOMBARDMENT) {
            val ammoColor = if (bombardmentAmmo > 0) 0xFFFFAA00.toInt() else 0xFFAA3333.toInt()
            guiGraphics.drawString(
                font, "×$bombardmentAmmo",
                mouseX + 10, mouseY - 12, ammoColor, true
            )
            return
        }

        val scale = CoordinateConverter.scaleFromZoom(zoom)
        val wX = CoordinateConverter.screenToWorldX(mouseX.toDouble(), mapCenterX, viewBlockX, scale)
        val wZ = CoordinateConverter.screenToWorldY(mouseY.toDouble(), mapCenterY, viewBlockZ, scale)
        val minDist = minSourceDistance(wX, wZ)
        val outOfRange = minDist > maxGuidedRange

        // Attack cursor icon
        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        if (directAmmo <= 0 || outOfRange) {
            RenderSystem.setShaderColor(0.5f, 0.15f, 0.1f, 0.7f)
        } else {
            RenderSystem.setShaderColor(1f, 0.4f, 0.1f, 0.9f)
        }

        guiGraphics.blit(ATTACK_CURSOR, mouseX - 8, mouseY - 8, 0f, 0f, 16, 16, 16, 16)
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        // Ammo count next to cursor
        guiGraphics.drawString(
            font, "×$directAmmo",
            mouseX + 10, mouseY - 12,
            if (directAmmo > 0) 0xFFFFAA00.toInt() else 0xFFAA3333.toInt(), true
        )

        // Info box at cursor top-right (12px offset)
        val boxX = mouseX + 12
        val boxY = mouseY - 12
        val distStr = formatDist(minDist)
        val rangeStr = formatDist(maxGuidedRange)
        val distText = if (outOfRange)
            Component.translatable("context.superbwarfare.tactical_map.attack_dist_out", distStr).string
        else
            Component.translatable("context.superbwarfare.tactical_map.attack_dist", distStr).string
        val rangeText = Component.translatable("context.superbwarfare.tactical_map.attack_range", rangeStr).string
        val distColor = if (outOfRange) 0xFFFF4444.toInt() else 0xFFCCCCCC.toInt()
        val rangeColor = 0xFFAAAAAA.toInt()

        val distW = font.width(distText)
        val rangeW = font.width(rangeText)
        val boxW = maxOf(distW, rangeW) + 6
        val boxH = font.lineHeight * 2 + 4
        guiGraphics.fill(boxX, boxY - boxH, boxX + boxW, boxY, 0xCC1A1A1A.toInt())
        guiGraphics.drawString(font, distText, boxX + 3, boxY - boxH + 2, distColor, false)
        guiGraphics.drawString(font, rangeText, boxX + 3, boxY - font.lineHeight, rangeColor, false)
    }

    private fun minSourceDistance(worldX: Double, worldZ: Double): Double {
        if (sourcePositions.isEmpty()) return 0.0
        val minDistSq = sourcePositions.minOf { pos ->
            val dx = worldX - pos.x
            val dz = worldZ - pos.z
            dx * dx + dz * dz
        }
        return sqrt(minDistSq)
    }

    private fun isOutOfRange(worldX: Double, worldZ: Double): Boolean {
        return minSourceDistance(worldX, worldZ) > maxGuidedRange
    }

    private fun formatDist(meters: Double): String {
        return if (meters >= 1000.0) "%.1fkm".format(meters / 1000.0) else "%.1fm".format(meters)
    }

    /** Renders a highlighted overlay on the currently hovered selection box in BOMBARDMENT mode. */
    fun renderBombardmentBoxHighlight(
        guiGraphics: GuiGraphics,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, zoom: Double
    ) {
        if (mode != Mode.BOMBARDMENT) return
        val box = hoveredBombardBox ?: return
        val scale = CoordinateConverter.scaleFromZoom(zoom)
        // Match SelectionBoxManager.render coordinate conversion exactly
        val minSX = CoordinateConverter.worldToScreenX(box.worldMinX, mapCenterX, viewBlockX, scale).toFloat()
        val maxSX = CoordinateConverter.worldToScreenX(box.worldMaxX, mapCenterX, viewBlockX, scale).toFloat()
        val minSY = CoordinateConverter.worldToScreenY(box.worldMinZ, mapCenterY, viewBlockZ, scale).toFloat()
        val maxSY = CoordinateConverter.worldToScreenY(box.worldMaxZ, mapCenterY, viewBlockZ, scale).toFloat()

        // +1 on right/bottom to cover the 1px white border drawn by SelectionBoxManager
        val l = minSX.toInt()
        val r = maxSX.toInt() + 1
        val t = minSY.toInt()
        val b = maxSY.toInt() + 1

        // Semi-transparent orange fill (matches selection box fill area exactly)
        guiGraphics.fill(l, t, r, b, 0x28FF6600)

        // Bright orange border (2px thick) drawn inside the exact box edges
        val borderColor = if (bombardmentAmmo > 0) 0xEEFF6600.toInt() else 0xEE883333.toInt()
        val thickness = 2
        guiGraphics.fill(l, t, r, t + thickness, borderColor)
        guiGraphics.fill(l, b - thickness, r, b, borderColor)
        guiGraphics.fill(l, t, l + thickness, b, borderColor)
        guiGraphics.fill(r - thickness, t, r, b, borderColor)
    }

    fun renderQueueTargets(
        guiGraphics: GuiGraphics,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, zoom: Double, font: Font
    ) {
        val scale = CoordinateConverter.scaleFromZoom(zoom)

        RenderSystem.enableBlend()
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        for ((i, pos) in targetQueue.withIndex()) {
            val sx = CoordinateConverter.worldToScreenX(pos.x + 0.5, mapCenterX, viewBlockX, scale)
            val sy = CoordinateConverter.worldToScreenY(pos.z + 0.5, mapCenterY, viewBlockZ, scale)
            val ix = sx.toFloat() - 8f
            val iy = sy.toFloat() - 8f
            RenderSystem.setShaderColor(1f, 0.67f, 0.1f, 0.9f)
            guiGraphics.blit(TARGET_FRAME, ix.roundToInt(), iy.roundToInt(), 0f, 0f, 16, 16, 16, 16)
            val num = "${i + 1}"
            val nw = font.width(num)
            guiGraphics.drawString(
                font, num,
                (sx - nw / 2f).roundToInt(), (sy - font.lineHeight / 2f).roundToInt(),
                0xFFFFFFFF.toInt(), false
            )
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    fun renderQueueMenu(
        guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int,
        font: Font, screenWidth: Int, screenHeight: Int
    ) {
        val items = listOf(
            Component.translatable("context.superbwarfare.tactical_map.sequential_fire").string,
            Component.translatable("context.superbwarfare.tactical_map.cancel_queue").string,
        )
        if (targetQueue.isEmpty()) {
            val label = Component.translatable("context.superbwarfare.tactical_map.cancel_queue").string
            val pw = font.width(label) + 8
            val ph = 14
            var mx = queueMenuX + 8
            var my = queueMenuY
            if (mx + pw > screenWidth) mx = queueMenuX - pw - 8
            if (my + ph > screenHeight) my = screenHeight - ph - 4
            val hovered = mouseX in mx..mx + pw && mouseY in my..my + ph
            guiGraphics.fill(mx, my, mx + pw, my + ph, 0xEE2A2A2A.toInt())
            if (hovered) guiGraphics.fill(mx + 1, my, mx + pw - 1, my + ph, 0x66444444)
            guiGraphics.drawString(
                font, label, mx + 4, my + 3,
                if (hovered) 0xFFFF5555.toInt() else 0xFFCC6666.toInt(), false
            )
            return
        }
        val padding = 4
        val itemHeight = 12
        val menuW = items.maxOf { font.width(it) } + padding * 2
        val menuH = items.size * itemHeight + padding * 2 + 2
        var mx = queueMenuX + 8
        var my = queueMenuY
        if (mx + menuW > screenWidth) mx = queueMenuX - menuW - 8
        if (my + menuH > screenHeight) my = screenHeight - menuH - 4
        val noAmmo = (onGetAmmo?.invoke(weaponName ?: "") ?: 0) <= 0
        val disabled = noAmmo || seqFireActive
        guiGraphics.fill(mx, my, mx + menuW, my + menuH, 0xEE2A2A2A.toInt())
        for ((i, label) in items.withIndex()) {
            val iy = my + padding + i * itemHeight
            val hovered = mouseX in mx..mx + menuW && mouseY in iy..iy + itemHeight
            val isSeqFire = i == 0
            val itemColor = if (isSeqFire && disabled) 0xFF666666.toInt()
            else if (hovered) 0xFFFFFFFF.toInt() else 0xFFCCCCCC.toInt()
            if (hovered && (!isSeqFire || !disabled)) {
                guiGraphics.fill(mx + 1, iy, mx + menuW - 1, iy + itemHeight, 0x664444FF)
            }
            guiGraphics.drawString(font, label, mx + padding, iy + 2, itemColor, false)
            if (hovered && isSeqFire && disabled) {
                val tip = if (seqFireActive)
                    Component.translatable("context.superbwarfare.tactical_map.firing")
                else Component.translatable("context.superbwarfare.tactical_map.missile_no_ammo")
                guiGraphics.renderTooltip(font, listOf(tip), Optional.empty(), mouseX, mouseY)
            }
        }
    }

    // ── Click handling ──

    /** 处理攻击模式下的鼠标点击。返回 true 表示已消费。 */
    fun handleClick(
        mouseX: Double, mouseY: Double, button: Int,
        isMouseInPanel: Boolean,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, zoom: Double,
        level: Level,
    ): Boolean {
        val scale = CoordinateConverter.scaleFromZoom(zoom)

        if (mode == Mode.DIRECT) {
            if (button == 0 && isMouseInPanel) {
                if ((onGetAmmo?.invoke(weaponName ?: "") ?: 0) <= 0) return true
                val wX = CoordinateConverter.screenToWorldX(mouseX, mapCenterX, viewBlockX, scale).toInt()
                val wZ = CoordinateConverter.screenToWorldY(mouseY, mapCenterY, viewBlockZ, scale).toInt()
                if (isOutOfRange(wX.toDouble(), wZ.toDouble())) return true
                val wY = lookupHeight(wX, wZ, level)
                onFireMissile?.invoke(wX, wY, wZ, weaponName!!)
                return true
            }
            if (button == 1) {
                exitMode()
                return true
            }
            return true
        }

        if (mode == Mode.QUEUE) {
            if (button == 0 && isMouseInPanel) {
                if (targetQueue.size >= (onGetAmmo?.invoke(weaponName ?: "") ?: 0)) return true
                val wX = CoordinateConverter.screenToWorldX(mouseX, mapCenterX, viewBlockX, scale).toInt()
                val wZ = CoordinateConverter.screenToWorldY(mouseY, mapCenterY, viewBlockZ, scale).toInt()
                if (isOutOfRange(wX.toDouble(), wZ.toDouble())) return true
                val wY = lookupHeight(wX, wZ, level)
                targetQueue.add(BlockPos(wX, wY, wZ))
                return true
            }
            if (button == 1) {
                queueMenuX = mouseX.toInt()
                queueMenuY = mouseY.toInt()
                queueMenuVisible = true
                return true
            }
            return true
        }

        if (mode == Mode.BOMBARDMENT) {
            if (button == 0 && isMouseInPanel) {
                if ((onGetAmmo?.invoke(weaponName ?: "") ?: 0) <= 0) return true
                val box = hoveredBombardBox ?: return true
                // Generate random position within the selection box
                val randX = box.worldMinX + Random.nextDouble() * (box.worldMaxX - box.worldMinX)
                val randZ = box.worldMinZ + Random.nextDouble() * (box.worldMaxZ - box.worldMinZ)
                if (isOutOfRange(randX, randZ)) return true
                val wX = randX.toInt()
                val wZ = randZ.toInt()
                val wY = lookupHeight(wX, wZ, level)
                onFireMissile?.invoke(wX, wY, wZ, weaponName!!)
                return true
            }
            if (button == 1) {
                exitMode()
                return true
            }
            return true
        }
        return false
    }

    /** 处理队列菜单点击。返回 true 表示已消费。 */
    fun handleQueueMenuClick(
        mouseX: Double, mouseY: Double,
        font: Font, screenWidth: Int, screenHeight: Int
    ): Boolean {
        if (!queueMenuVisible) return false

        if (targetQueue.isEmpty()) {
            val label = Component.translatable("context.superbwarfare.tactical_map.cancel_queue").string
            val pw = font.width(label) + 8
            val ph = 14
            var mx = queueMenuX + 8
            var my = queueMenuY
            if (mx + pw > screenWidth) mx = queueMenuX - pw - 8
            if (my + ph > screenHeight) my = screenHeight - ph - 4
            if (mouseX in mx.toDouble()..(mx + pw).toDouble() && mouseY in my.toDouble()..(my + ph).toDouble()) {
                exitMode()
                return true
            }
            queueMenuVisible = false
            return true
        }

        val items = listOf(
            Component.translatable("context.superbwarfare.tactical_map.sequential_fire").string,
            Component.translatable("context.superbwarfare.tactical_map.cancel_queue").string,
        )
        val padding = 4
        val itemHeight = 12
        val menuW = items.maxOf { font.width(it) } + padding * 2
        val menuH = items.size * itemHeight + padding * 2 + 2
        var mx = queueMenuX + 8
        var my = queueMenuY
        if (mx + menuW > screenWidth) mx = queueMenuX - menuW - 8
        if (my + menuH > screenHeight) my = screenHeight - menuH - 4

        for ((i, _) in items.withIndex()) {
            val iy = my + padding + i * itemHeight
            if (mouseX in mx.toDouble()..(mx + menuW).toDouble() && mouseY in iy.toDouble()..(iy + itemHeight).toDouble()) {
                when (i) {
                    0 -> if (!seqFireActive && (onGetAmmo?.invoke(weaponName ?: "") ?: 0) > 0) {
                        queueMenuVisible = false
                        startSequentialFire()
                    }

                    1 -> {
                        queueMenuVisible = false
                        exitMode()
                    }
                }
                return true
            }
        }
        queueMenuVisible = false
        return true
    }

    // ── Sequential fire ──

    private fun startSequentialFire() {
        if (targetQueue.isEmpty()) return
        seqFireActive = true
        seqFireIndex = 0
        seqFireTimer = 0
    }

    // ── Height lookup ──

    private fun lookupHeight(wX: Int, wZ: Int, level: Level): Int {
        val chunk = level.getChunk(wX shr 4, wZ shr 4)
        return if (chunk is LevelChunk && !chunk.isEmpty)
            level.getHeight(Heightmap.Types.WORLD_SURFACE, wX, wZ)
        else TacticalMapCache.getCachedHeight(wX, wZ)?.toInt()
            ?: (Minecraft.getInstance().player?.blockY ?: 64)
    }
}
