package com.atsuishio.superbwarfare.client.screens.map

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

/**
 * 战术地图实体图标渲染器。
 * 负责所有实体（友方/中立/敌方）、玩家标记、雷达扇面、导弹目标连线、盘旋巡航点的渲染。
 *
 * 注意：此类的渲染方法假设 RenderSystem 的 scissor / blendFunc / depthMask / setShader
 * 已由调用方（Screen.render()）设置好，仅在其内部做颜色/透明度微调。
 */
class MapEntityRenderer {

    data class EntityRenderEntry(val entity: Entity, val screenX: Float, val screenY: Float, val relation: String)

    // ── Textures ──
    companion object {
        private val PLAYER_MARKER = loc("textures/overlay/tactical_map/player_marker.png")
        private val TEAMMATE_MARKER = loc("textures/overlay/tactical_map/vehicle/indicator.png")
        private val PLAYER_DIRECTION = loc("textures/overlay/tactical_map/player_direction.png")
        private val RADAR_ICON = loc("textures/overlay/tactical_map/radar.png")
        private val TARGET_POS = loc("textures/overlay/tactical_map/target_pos.png")
        private val CRUISE_MARKER = loc("textures/overlay/tactical_map/cruise_marker.png")
        private val SEL_TARGET = loc("textures/overlay/tactical_map/sel_target.png")

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

        /** Liang-Barsky 线裁剪到地图可视区域，返回本地虚线坐标范围 */
        fun clipDashRange(sx: Float, sy: Float, ex: Float, ey: Float,
                          mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int): Pair<Int, Int>? {
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
    }

    // ═══════════════════════════════════════════════════════════════
    //  Batch entity rendering (replaces triplicated loops)
    // ═══════════════════════════════════════════════════════════════

    /**
     * 批量渲染一批同类实体（友方/中立/敌方）。
     * 替代原来的三次重复循环。
     */
    fun renderEntityBatch(
        guiGraphics: GuiGraphics,
        entities: List<Entity>,
        level: Level,
        tintColor: Int,
        relationKey: String,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int,
        scale: Double, pPartialTick: Float,
        mouseX: Int, mouseY: Int,
        selectedEntities: List<Entity>,
        outEntries: MutableList<EntityRenderEntry>,
        onHover: (lines: List<Component>, tipX: Int, tipY: Int) -> Unit,
        isDraggingLoiter: Boolean, loiterDragNewX: Double, loiterDragNewZ: Double,
        loiterDragExpireTime: Long,
    ) {
        val iconSize = 12
        val half = iconSize / 2

        for (e in entities) {
            val entity = level.getEntity(e.id) ?: e
            val screenX = CoordinateConverter.worldToScreenX(entity.x, mapCenterX, viewBlockX, scale).toFloat()
            val screenY = CoordinateConverter.worldToScreenY(entity.z, mapCenterY, viewBlockZ, scale).toFloat()
            val (clampedX, clampedY) = CoordinateConverter.clampToMapArea(
                screenX.toDouble(), screenY.toDouble(), mapLeft, mapTop, mapAreaW, mapAreaH
            )
            renderMapEntity(entity, level, scale, pPartialTick, guiGraphics, tintColor,
                viewBlockX, viewBlockZ, mapCenterX, mapCenterY, mapLeft, mapTop, mapAreaW, mapAreaH,
                isDraggingLoiter, loiterDragNewX, loiterDragNewZ, loiterDragExpireTime)

            if (selectedEntities.any { it.id == entity.id })
                drawSelectedBorder(guiGraphics, clampedX, clampedY)

            if (mouseX.toFloat() in (clampedX - half)..(clampedX + half) &&
                mouseY.toFloat() in (clampedY - half)..(clampedY + half)
            ) {
                onHover(buildEntityTooltip(entity, level, relationKey), mouseX, mouseY)
            }

            outEntries.add(EntityRenderEntry(entity, clampedX, clampedY, relationKey.removePrefix("context.superbwarfare.tactical_map.relation.")))
        }
    }

    private fun buildEntityTooltip(entity: Entity, level: Level, relationKey: String): List<Component> {
        val lines = mutableListOf<Component>()
        lines.add(Component.translatable(
            "context.superbwarfare.tactical_map.tooltip.name", entity.displayName
        ).withStyle(net.minecraft.ChatFormatting.WHITE))
        lines.add(Component.translatable(
            "context.superbwarfare.tactical_map.tooltip.pos",
            entity.x.toInt().toString(), entity.y.toInt().toString(), entity.z.toInt().toString()
        ).withStyle(net.minecraft.ChatFormatting.GRAY))
        val teamName = (entity as? LivingEntity)?.team?.name
            ?: (entity as? VehicleEntity)?.lastDriver?.let { (it as? LivingEntity)?.team?.name }
        if (!teamName.isNullOrEmpty()) {
            lines.add(Component.translatable(
                "context.superbwarfare.tactical_map.tooltip.team", teamName
            ).withStyle(net.minecraft.ChatFormatting.AQUA))
        }
        val syncedEntry = ClientSyncedEntityHandler.getSyncedEntry(level, entity.id)
        val hag = syncedEntry?.heightAboveGround ?: -1.0
        lines.add(if (hag >= 0)
            Component.translatable("context.superbwarfare.tactical_map.tooltip.height", "%.1f".format(hag))
            else Component.translatable("context.superbwarfare.tactical_map.tooltip.height_na"))
        // Missile speed in Mach: delta between current and previous synced position
        if (entity is MissileProjectile) {
            val pp = syncedEntry?.prevPos
            val speedMs = if (pp != null) {
                val dx = entity.x - pp.x
                val dy = entity.y - pp.y
                val dz = entity.z - pp.z
                kotlin.math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0
            } else 0.0
            val mach = speedMs / 340.0
            lines.add(Component.translatable("context.superbwarfare.tactical_map.tooltip.speed",
                "%.1f".format(mach)).withStyle(net.minecraft.ChatFormatting.GOLD))
        }
        lines.add(Component.translatable(relationKey).withStyle(net.minecraft.ChatFormatting.YELLOW))
        return lines
    }

    // ═══════════════════════════════════════════════════════════════
    //  Single entity rendering
    // ═══════════════════════════════════════════════════════════════

    fun renderMapEntity(
        entity: Entity,
        level: Level,
        scale: Double,
        pPartialTick: Float,
        guiGraphics: GuiGraphics,
        tintColor: Int,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int,
        isDraggingLoiter: Boolean = false, loiterDragNewX: Double = 0.0, loiterDragNewZ: Double = 0.0,
        loiterDragExpireTime: Long = 0L,
    ) {
        val r = ((tintColor shr 16) and 0xFF) / 255f
        val g = ((tintColor shr 8) and 0xFF) / 255f
        val b = (tintColor and 0xFF) / 255f

        val screenX = CoordinateConverter.worldToScreenX(entity.x, mapCenterX, viewBlockX, scale).toFloat()
        val screenY = CoordinateConverter.worldToScreenY(entity.z, mapCenterY, viewBlockZ, scale).toFloat()
        val icon = getVehicleIcon(entity)
        val iconSize = 12

        val (clampedX, clampedY) = CoordinateConverter.clampToMapArea(
            screenX.toDouble(), screenY.toDouble(), mapLeft, mapTop, mapAreaW, mapAreaH
        )
        val alpha = if (screenX == clampedX && screenY == clampedY) 1f else 0.5f

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

        // 导弹目标位置
        if (entity is MissileProjectile) {
            val synced = ClientSyncedEntityHandler.getSyncedEntry(level, entity.id)
            val targetPos = synced?.targetPos ?: entity.getTargetPos()
            if (targetPos != null) {
                renderTargetPos(targetPos, scale, screenX, screenY, guiGraphics, entity,
                    viewBlockX, viewBlockZ, mapCenterX, mapCenterY, mapLeft, mapTop, mapAreaW, mapAreaH)
            }
        }

        // 盘旋巡航点
        val player = localPlayer
        if (entity is VehicleEntity && entity.loiterActive && player != null && player.vehicle === entity) {
            val useDragPos = isDraggingLoiter || System.currentTimeMillis() < loiterDragExpireTime
            val lx = if (useDragPos) loiterDragNewX else entity.loiterCenterX
            val lz = if (useDragPos) loiterDragNewZ else entity.loiterCenterZ
            val navScreenX = CoordinateConverter.worldToScreenX(lx, mapCenterX, viewBlockX, scale)
            val navScreenY = CoordinateConverter.worldToScreenY(lz, mapCenterY, viewBlockZ, scale)

            val ldx = navScreenX - screenX
            val ldy = navScreenY - screenY
            val len = kotlin.math.sqrt((ldx * ldx + ldy * ldy).toDouble()).toFloat()
            if (len > 2f) {
                val range = clipDashRange(screenX, screenY, navScreenX.toFloat(), navScreenY.toFloat(), mapLeft, mapTop, mapAreaW, mapAreaH)
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
                    val font = net.minecraft.client.Minecraft.getInstance().font
                    val dist = kotlin.math.sqrt((lx - entity.x) * (lx - entity.x) + (lz - entity.z) * (lz - entity.z))
                    val label = "${dist.toInt()}m"
                    guiGraphics.drawString(font, label, (navScreenX + 10).toInt(), (navScreenY + 6).toInt(), 0xFFCDFFF6.toInt(), true)
                }
            }

            val clampedNX = navScreenX.coerceIn((mapLeft + 4).toDouble(), (mapLeft + mapAreaW - 4).toDouble()).toFloat()
            val clampedNY = navScreenY.coerceIn((mapTop + 13).toDouble(), (mapTop + mapAreaH).toDouble()).toFloat()
            val navPose = guiGraphics.pose()
            navPose.pushPose()
            navPose.translate(clampedNX, clampedNY, 0f)
            guiGraphics.blit(CRUISE_MARKER, -4, -13, 0f, 0f, 8, 13, 8, 13)
            navPose.popPose()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Player marker
    // ═══════════════════════════════════════════════════════════════

    fun renderPlayerMarker(guiGraphics: GuiGraphics, player: Player,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, scale: Double
    ) {
        if (player.vehicle is VehicleEntity) return

        val px = CoordinateConverter.worldToScreenX(player.x, mapCenterX, viewBlockX, scale).toFloat()
        val py = CoordinateConverter.worldToScreenY(player.z, mapCenterY, viewBlockZ, scale).toFloat()

        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(px, py, 0f)
        pose.rotateAround(Axis.ZP.rotationDegrees(player.yRot + 180f), 0f, 0f, 0f)
        guiGraphics.blit(PLAYER_MARKER, -6, -6, 0f, 0f, 12, 12, 12, 12)
        pose.popPose()
    }

    fun renderPlayerOffscreenIndicator(guiGraphics: GuiGraphics, player: Player,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float, scale: Double,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int
    ) {
        val px = CoordinateConverter.worldToScreenX(player.x, mapCenterX, viewBlockX, scale).toFloat()
        val py = CoordinateConverter.worldToScreenY(player.z, mapCenterY, viewBlockZ, scale).toFloat()

        val inBounds = px >= mapLeft && px <= mapLeft + mapAreaW &&
            py >= mapTop && py <= mapTop + mapAreaH
        if (inBounds) return

        val iconSize = 8
        val half = iconSize / 2
        val edgeX = px.coerceIn((mapLeft + half).toFloat(), (mapLeft + mapAreaW - half).toFloat())
        val edgeY = py.coerceIn((mapTop + half).toFloat(), (mapTop + mapAreaH - half).toFloat())
        val angle = Math.toDegrees(atan2((py - mapCenterY).toDouble(), (px - mapCenterX).toDouble())).toFloat()

        RenderSystem.setShaderColor(1f, 1f, 1f, 0.5f)
        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(edgeX, edgeY, 0f)
        pose.rotateAround(Axis.ZP.rotationDegrees(angle - 90f), 0f, 0f, 0f)
        guiGraphics.blit(PLAYER_DIRECTION, -half, -half, 0f, 0f, iconSize, iconSize, iconSize, iconSize)
        pose.popPose()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════
    //  Radar
    // ═══════════════════════════════════════════════════════════════

    fun renderRadarsIcon(level: Level, scale: Double, guiGraphics: GuiGraphics,
        mapCenterX: Float, mapCenterY: Float, viewBlockX: Double, viewBlockZ: Double
    ) {
        val radars = ClientSyncedEntityHandler.getSyncedRadars(level)
        if (radars.isEmpty()) return

        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        for (radar in radars) {
            if (radar.showIcon) {
                val rx = CoordinateConverter.worldToScreenX(radar.pos.x, mapCenterX, viewBlockX, scale).toFloat()
                val ry = CoordinateConverter.worldToScreenY(radar.pos.z, mapCenterY, viewBlockZ, scale).toFloat()
                val iconSize = 8
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.9f)
                guiGraphics.blit(RADAR_ICON,
                    (rx - iconSize / 2).toInt(), (ry - iconSize / 2).toInt(),
                    0f, 0f, iconSize, iconSize, iconSize, iconSize)
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    fun renderRadars(level: Level, scale: Double, guiGraphics: GuiGraphics,
        mapCenterX: Float, mapCenterY: Float, viewBlockX: Double, viewBlockZ: Double
    ) {
        val radars = ClientSyncedEntityHandler.getSyncedRadars(level)
        if (radars.isEmpty()) return

        RenderSystem.setShader { GameRenderer.getPositionShader() }
        for (radar in radars) {
            val rx = CoordinateConverter.worldToScreenX(radar.pos.x, mapCenterX, viewBlockX, scale).toFloat()
            val ry = CoordinateConverter.worldToScreenY(radar.pos.z, mapCenterY, viewBlockZ, scale).toFloat()
            val pr = (radar.radius * scale).toFloat()
            val startAngle = (radar.yRot - radar.sweepAngle / 2.0 - 90.0).toFloat()
            val sweep = radar.sweepAngle.toFloat().coerceIn(0f, 360f)
            drawFilledSector(guiGraphics, rx, ry, pr, startAngle, sweep)
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    fun drawFilledSector(guiGraphics: GuiGraphics, cx: Float, cy: Float, radius: Float, startDeg: Float, sweepDeg: Float) {
        if (radius < 2f || sweepDeg <= 0f) return
        RenderSystem.setShaderColor(0f, 1f, 0f, 0.2f)

        val pose = guiGraphics.pose()
        pose.pushPose()
        pose.translate(cx, cy, 0f)

        val tess = com.mojang.blaze3d.vertex.Tesselator.getInstance()
        val buf = tess.builder
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

    // ═══════════════════════════════════════════════════════════════
    //  Target position (missile target line + breathing icon)
    // ═══════════════════════════════════════════════════════════════

    fun renderTargetPos(targetPos: Vec3, scale: Double, screenX: Float, screenY: Float,
        guiGraphics: GuiGraphics, entity: Entity,
        viewBlockX: Double, viewBlockZ: Double,
        mapCenterX: Float, mapCenterY: Float,
        mapLeft: Int, mapTop: Int, mapAreaW: Int, mapAreaH: Int
    ) {
        val tdx = targetPos.x - viewBlockX
        val tdz = targetPos.z - viewBlockZ
        val targetScreenX = CoordinateConverter.worldToScreenX(targetPos.x, mapCenterX, viewBlockX, scale).toFloat()
        val targetScreenY = CoordinateConverter.worldToScreenY(targetPos.z, mapCenterY, viewBlockZ, scale).toFloat()

        val ldx = targetScreenX - screenX
        val ldy = targetScreenY - screenY
        val len = kotlin.math.sqrt((ldx * ldx + ldy * ldy).toDouble()).toFloat()
        if (len > 2f) {
            val range = clipDashRange(screenX, screenY, targetScreenX, targetScreenY, mapLeft, mapTop, mapAreaW, mapAreaH) ?: return
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

            val font = net.minecraft.client.Minecraft.getInstance().font
            val dist = kotlin.math.sqrt((targetPos.x - entity.x) * (targetPos.x - entity.x) + (targetPos.z - entity.z) * (targetPos.z - entity.z))
            val label = "${dist.toInt()}m"
            guiGraphics.drawString(font, label, (targetScreenX + 10).toInt(), (targetScreenY + 6).toInt(), 0xFFFF0000.toInt(), true)
        }

        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        val time = System.currentTimeMillis() / 1000.0
        val breathScale = (1.0 + 0.25 * kotlin.math.sin(time * 4.0)).toFloat()
        val targetPose = guiGraphics.pose()
        targetPose.pushPose()
        targetPose.translate(targetScreenX, targetScreenY, 0f)
        targetPose.scale(breathScale, breathScale, 1f)
        guiGraphics.blit(TARGET_POS, -8, -8, 0f, 0f, 16, 16, 16, 16)
        targetPose.popPose()
    }

    // ═══════════════════════════════════════════════════════════════
    //  Selection border
    // ═══════════════════════════════════════════════════════════════

    fun drawSelectedBorder(guiGraphics: GuiGraphics, centerX: Float, centerY: Float) {
        guiGraphics.blit(SEL_TARGET, (centerX - 8).toInt(), (centerY - 8).toInt(), 0f, 0f, 16, 16, 16, 16)
    }

    // ═══════════════════════════════════════════════════════════════
    //  Vehicle icon lookup
    // ═══════════════════════════════════════════════════════════════

    fun getVehicleIcon(entity: Entity): ResourceLocation {
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

}
