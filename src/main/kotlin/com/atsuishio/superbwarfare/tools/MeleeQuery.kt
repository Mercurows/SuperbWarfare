package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.data.gun.melee.*
import com.atsuishio.superbwarfare.tools.MeleeQuery.SEGMENT_SAMPLES
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.*

/**
 * 近战判定的**唯一实现**（形状 + 扫掠 + 排序/数量/衰减 + 命中区域）。
 *
 * **只在客户端调用**（与现状相同的信任模型：判定在客户端算，服务端只结算）。
 * 做成独立纯函数类是为了可单测、可被调试工具复用、改动只动一处。
 *
 * 判定流程与旧的 `doGunMeleeAttack` 相比有**三处有意修正**：
 * 1. 距离改为「到目标 **AABB 最近点**」而不是脚底；
 * 2. 遮挡由 `MeleeHitbox.Occlusion` 统一（旧实现里方块 pick 是死代码、能隔墙打人）；
 * 3. 俯仰角可单独限制，且角度参照点统一（旧实现用「眼→眼」夹角，现在是「视线 ↔ 到最近点」）。
 *
 * 粗筛仍是 `SeekTool.BASIC_FILTER` + `NOT_IN_SMOKE` + 同队排除，
 * 并统一排除「自己骑的载具」。
 */
object MeleeQuery {

    /**
     * 一次近战判定的命中结果。
     *
     * @param entity     命中的实体
     * @param hitPos     判定体到目标 AABB 的入射点（打头/打腿判定用它；已在判定体内时退化为 AABB 中心）
     * @param distance   眼睛到该点的距离
     * @param angle      视线与「眼睛 → 该点」的夹角（度）
     * @param sampleIndex 是第几个扫掠采样点命中的（0 = 第一次采样）
     * @param order      在本次结果里的排序下标（服务端按它做衰减）
     */
    data class Hit(
        val entity: Entity,
        val hitPos: Vec3,
        val distance: Double,
        val angle: Double,
        val sampleIndex: Int,
        val order: Int,
        val headshot: Boolean,
        val legshot: Boolean,
    )

    /**
     * 判定参数完全展开后的结构，供调试渲染与 [sampleOffsets] 复用。
     *
     * @param eyePos  结算 tick 的玩家眼睛位置（方向基准 = 玩家当前朝向，方案 1）
     * @param yaw     玩家当前 yaw
     * @param pitch   玩家当前 pitch
     * @param reach   总距离 = `action.hitbox.range + player.getEntityReach()`
     */
    data class Context(
        val eyePos: Vec3,
        val yaw: Float,
        val pitch: Float,
        val reach: Double,
    )

    /** 用玩家结算 tick 的当前朝向构造判定上下文 */
    @JvmStatic
    fun contextOf(player: Player, action: ResolvedMeleeAction): Context {
        return Context(
            eyePos = player.eyePosition,
            yaw = player.yRot,
            pitch = player.xRot,
            reach = action.hitbox.range + player.getEntityReach(),
        )
    }

    /**
     * 完整判定：粗筛 → 逐个形状精判 → 排序 → 截断 → 命中区域。
     *
     * @param candidates 粗筛结果；传 `null` 时由本方法按 [Context.reach] 自行粗筛
     */
    @JvmStatic
    @JvmOverloads
    fun resolve(
        level: Level,
        attacker: Entity,
        action: ResolvedMeleeAction,
        context: Context? = null,
        candidates: List<Entity>? = null,
    ): List<Hit> {
        val player = attacker as? Player ?: return emptyList()
        val ctx = context ?: contextOf(player, action)

        val entityList = candidates ?: coarseFilter(level, attacker, ctx.reach)

        val hitbox = action.hitbox
        val offsets = action.sweep?.sampleOffsets() ?: listOf(0.0)
        val byEntity = LinkedHashMap<Entity, Hit>()

        for ((sampleIndex, offset) in offsets.withIndex()) {
            val yaw = (ctx.yaw + offset).toFloat()
            val look = lookVector(yaw, ctx.pitch)

            for (entity in entityList) {
                if (byEntity.containsKey(entity)) continue
                val box = entity.boundingBox
                if (box.xsize <= 0.0 || box.ysize <= 0.0 || box.zsize <= 0.0) continue

                val hitPos = when (hitbox.type) {
                    MeleeHitboxType.CONE -> coneHit(ctx.eyePos, yaw, ctx.pitch, box, hitbox, ctx.reach)
                    MeleeHitboxType.BOX -> boxHit(ctx.eyePos, yaw, look, box, hitbox)
                    MeleeHitboxType.CAPSULE -> capsuleHit(ctx.eyePos, look, box, hitbox, ctx.reach)
                } ?: continue

                if (hitbox.occlusion && !hasLineOfSight(level, attacker, ctx.eyePos, hitPos)) continue

                val delta = ctx.eyePos.vectorTo(hitPos)
                byEntity[entity] = Hit(
                    entity = entity,
                    hitPos = hitPos,
                    distance = delta.length(),
                    angle = angleBetween(lookVector(yaw, ctx.pitch), delta),
                    sampleIndex = sampleIndex,
                    order = 0,
                    headshot = isHeadshot(entity, hitPos),
                    legshot = isLegshot(entity, hitPos),
                )
            }
        }

        val sorted = sort(byEntity.values, action.sortBy)
        val limited = if (action.maxTargets > 0) sorted.take(action.maxTargets) else sorted

        return limited.mapIndexed { index, hit -> hit.copy(order = index) }
    }

    /**
     * 粗筛：以玩家为原点、`reach + 扫掠外接半径` 构造 AABB。
     *
     * 过滤器沿用 [SeekTool.BASIC_FILTER] + `NOT_IN_SMOKE` + 同队排除，并排除自己骑的载具。
     */
    @JvmStatic
    fun coarseFilter(level: Level, attacker: Entity, reach: Double, sweep: MeleeSweep? = null): List<Entity> {
        val lateral = sweep?.maxLateral(reach) ?: 0.0
        val radius = reach + lateral + 1.0
        val aabb = AABB(
            attacker.x - radius, attacker.y - radius, attacker.z - radius,
            attacker.x + radius, attacker.y + radius, attacker.z + radius,
        )
        val vehicle = attacker.vehicle

        return level.getEntities(attacker, aabb) { e ->
            e !== attacker
                    && e !== vehicle
                    && SeekTool.BASIC_FILTER.test(e)
                    && SeekTool.NOT_IN_SMOKE.test(e)
                    && !SeekTool.IN_SAME_TEAM.test(attacker, e)
        }
    }

    // ------------------------------------------------------------------ 形状

    /**
     * 圆锥（兼容旧行为）：目标 AABB 最近点，距离 ≤ [MeleeHitbox.range]，
     * `|Δyaw| ≤ Angle/2` 且 `|Δpitch| ≤ Pitch/2`。
     */
    private fun coneHit(
        eyePos: Vec3,
        yaw: Float,
        pitch: Float,
        box: AABB,
        hitbox: MeleeHitbox,
        reach: Double,
    ): Vec3? {
        val point = closestPointInBox(box, eyePos)
        val delta = eyePos.vectorTo(point)
        if (delta.lengthSqr() > reach * reach) return null
        if (delta.lengthSqr() < EPSILON) return box.center

        val horizontal = sqrt(delta.x * delta.x + delta.z * delta.z)
        val targetYaw = Math.toDegrees(atan2(-delta.x, delta.z)).toFloat()
        val targetPitch = Math.toDegrees(asin((delta.y / delta.length()).coerceIn(-1.0, 1.0))).toFloat()

        if (abs(Mth.wrapDegrees(targetYaw - yaw)) > hitbox.angle / 2.0) return null
        if (horizontal > EPSILON && abs(Mth.wrapDegrees(targetPitch - pitch)) > hitbox.pitch / 2.0) return null

        return point
    }

    /**
     * 盒体：OBB（绕 Y 旋转 `yaw`，沿视线 `zFrom → zFrom + length`）∩ 目标 AABB。
     *
     * 直接用 [OBB] + [OBB.isColliding]，与载具碰撞判定同一套 SAT 实现。
     * 盒体自带明确尺寸（`Width`/`Height`/`Length`），所以**不再额外用 `reach` 收口**——
     * 否则 `range` 一写大就会把盒体判定放大成"看不见的远程攻击"。
     */
    private fun boxHit(
        eyePos: Vec3,
        yaw: Float,
        look: Vec3,
        box: AABB,
        hitbox: MeleeHitbox,
    ): Vec3? {
        val halfLength = max(hitbox.length, 0.0) / 2.0
        val center = eyePos
            .add(0.0, hitbox.yOffset, 0.0)
            .add(look.scale(hitbox.zFrom + halfLength))

        val obb = OBB(
            Vector3d(center.x, center.y, center.z),
            Vector3d(max(hitbox.width, 0.0) / 2.0, max(hitbox.height, 0.0) / 2.0, halfLength),
            Quaterniond().rotateY(Math.toRadians(yaw.toDouble())),
            OBB.Part.EMPTY,
        )

        if (!OBB.isColliding(obb, box)) return null

        return closestPointInBox(box, eyePos)
    }

    /**
     * 胶囊：线段（沿视线 `zFrom → zFrom + range`）到目标 AABB 的最近距离 ≤ `Radius`。
     */
    private fun capsuleHit(
        eyePos: Vec3,
        look: Vec3,
        box: AABB,
        hitbox: MeleeHitbox,
        reach: Double,
    ): Vec3? {
        val startOffset = hitbox.zFrom
        val endOffset = hitbox.zFrom + if (hitbox.range > 0) hitbox.range else reach
        val start = eyePos.add(look.scale(startOffset))
        val end = eyePos.add(look.scale(endOffset))

        val radius = max(hitbox.radius, 0.0)
        val (segmentPoint, boxPoint) = closestSegmentToBox(start, end, box)
        if (segmentPoint.distanceTo(boxPoint) > radius) return null

        return boxPoint
    }

    // ------------------------------------------------------------------ 几何工具

    /** MC 的朝向约定：yaw 绕 Y、pitch 向下为正，x = -sin(yaw)cos(pitch)、y = -sin(pitch)、z = cos(yaw)cos(pitch) */
    @JvmStatic
    fun lookVector(yaw: Float, pitch: Float): Vec3 {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        return Vec3(
            -sin(yawRad) * cos(pitchRad),
            -sin(pitchRad),
            cos(yawRad) * cos(pitchRad),
        )
    }

    /** 两个向量之间的夹角（度） */
    @JvmStatic
    fun angleBetween(from: Vec3, to: Vec3): Double {
        val len = from.length() * to.length()
        if (len < EPSILON) return 0.0
        val dot = (from.x * to.x + from.y * to.y + from.z * to.z) / len
        return Math.toDegrees(kotlin.math.acos(dot.coerceIn(-1.0, 1.0)))
    }

    /** 点 [point] 到 [box] 的最近点（点在盒内时返回 [point] 自身） */
    @JvmStatic
    fun closestPointInBox(box: AABB, point: Vec3): Vec3 {
        return Vec3(
            point.x.coerceIn(box.minX, box.maxX),
            point.y.coerceIn(box.minY, box.maxY),
            point.z.coerceIn(box.minZ, box.maxZ),
        )
    }

    /** 线段 `[start, end]` 上离 [point] 最近的点 */
    @JvmStatic
    fun closestPointOnSegment(start: Vec3, end: Vec3, point: Vec3): Vec3 {
        val direction = end.subtract(start)
        val lengthSqr = direction.lengthSqr()
        if (lengthSqr < EPSILON) return start
        val t = (point.subtract(start).dot(direction) / lengthSqr).coerceIn(0.0, 1.0)
        return start.add(direction.scale(t))
    }

    /**
     * 线段与 AABB 的最近点对。
     *
     * 用「分段细分 + 收敛」的近似做法：把线段切成 [SEGMENT_SAMPLES] 段，
     * 取离 AABB 最近的那一段再细分一次。对近战这种「最长几米」的线段精度足够（亚毫米级），
     * 但比逐面解的解析法短得多、也不容易在退化情形（线段完全在盒内/平行于某面）上出错。
     */
    private fun closestSegmentToBox(start: Vec3, end: Vec3, box: AABB): Pair<Vec3, Vec3> {
        var bestSegmentPoint = start
        var bestBoxPoint = closestPointInBox(box, start)
        var bestDistance = bestSegmentPoint.distanceToSqr(bestBoxPoint)

        var segStart = start
        var step = end.subtract(start).scale(1.0 / SEGMENT_SAMPLES)

        for (i in 0 until SEGMENT_SAMPLES) {
            val segEnd = if (i == SEGMENT_SAMPLES - 1) end else segStart.add(step)
            for (j in 0..SUBDIVISION_SAMPLES) {
                val point = segStart.add(segEnd.subtract(segStart).scale(j.toDouble() / SUBDIVISION_SAMPLES))
                val boxPoint = closestPointInBox(box, point)
                val distance = point.distanceToSqr(boxPoint)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestSegmentPoint = point
                    bestBoxPoint = boxPoint
                    if (distance < EPSILON) return bestSegmentPoint to bestBoxPoint
                }
            }
            segStart = segEnd
        }

        // 收敛：以找到的点为中心再细分一轮
        val refineStep = step.scale(1.0 / SUBDIVISION_SAMPLES)
        for (i in -SUBDIVISION_SAMPLES..SUBDIVISION_SAMPLES) {
            val point = bestSegmentPoint.add(refineStep.scale(i.toDouble()))
            val clamped = closestPointOnSegment(start, end, point)
            val boxPoint = closestPointInBox(box, clamped)
            val distance = clamped.distanceToSqr(boxPoint)
            if (distance < bestDistance) {
                bestDistance = distance
                bestSegmentPoint = clamped
                bestBoxPoint = boxPoint
            }
        }

        return bestSegmentPoint to bestBoxPoint
    }

    /** 视线是否通畅（用方块 COLLIDER 射线，不含流体） */
    private fun hasLineOfSight(level: Level, attacker: Entity, from: Vec3, to: Vec3): Boolean {
        if (from.distanceToSqr(to) < EPSILON) return true
        val result = level.clip(
            ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)
        )
        return result.type != HitResult.Type.BLOCK
    }

    /** 打头：复用投射物已验证的阈值（`ProjectileEntity` / `IAdvancedHitDetection`） */
    @JvmStatic
    fun isHeadshot(target: Entity, hitPos: Vec3): Boolean {
        val local = hitPos.y - target.y
        return local > (target.eyeHeight - HEADSHOT_MARGIN_BELOW) && local < (target.eyeHeight + HEADSHOT_MARGIN_ABOVE)
    }

    /** 打腿：`hitPos.y < 0.33 * bbHeight`（相对脚底） */
    @JvmStatic
    fun isLegshot(target: Entity, hitPos: Vec3): Boolean {
        return (hitPos.y - target.y) < LEGSHOT_RATIO * target.bbHeight
    }

    // ------------------------------------------------------------------ 排序

    private fun sort(hits: Collection<Hit>, sortBy: MeleeSortBy): List<Hit> {
        return when (sortBy) {
            MeleeSortBy.ANGLE -> hits.sortedWith(compareBy({ it.angle }, { it.distance }))
            MeleeSortBy.DISTANCE -> hits.sortedWith(compareBy({ it.distance }, { it.angle }))
            MeleeSortBy.SWEEP_ORDER -> hits.sortedWith(
                compareBy({ it.sampleIndex }, { it.angle }, { it.distance })
            )
        }
    }

    // ------------------------------------------------------------------ 调试

    /**
     * 判定体的调试线框：每个扫掠采样点一个 OBB。
     *
     * 三种形状统一成盒体近似（圆锥用外接盒），这样可视化只有一条代码路径；
     * `Cone`/`Capsule` 的近似盒会被标成非精确（见 [DebugBox.exact]）。
     */
    data class DebugBox(
        val center: Vec3,
        val radiusX: Double,
        val radiusY: Double,
        val radiusZ: Double,
        val yaw: Float,
        /** `true` = 这个形状本身就是盒体；`false` = 圆锥/胶囊的外接近似 */
        val exact: Boolean,
    )

    @JvmStatic
    fun debugBoxes(context: Context, action: ResolvedMeleeAction): List<DebugBox> {
        val hitbox = action.hitbox
        val offsets = action.sweep?.sampleOffsets() ?: listOf(0.0)

        return offsets.map { offset ->
            val yaw = (context.yaw + offset).toFloat()
            val look = lookVector(yaw, context.pitch)
            when (hitbox.type) {
                MeleeHitboxType.CONE -> {
                    // 圆锥：以总张角算末端半径，中心放在 reach/2 处
                    val half = context.reach * sin(Math.toRadians((hitbox.angle / 2).coerceAtMost(89.0))) / 2
                    val center = context.eyePos.add(look.scale(context.reach / 2))
                    DebugBox(center, half, half, context.reach / 2, yaw, false)
                }

                MeleeHitboxType.BOX -> {
                    val halfLength = max(hitbox.length, 0.0) / 2.0
                    val center = context.eyePos
                        .add(0.0, hitbox.yOffset, 0.0)
                        .add(look.scale(hitbox.zFrom + halfLength))
                    DebugBox(center, hitbox.width / 2, hitbox.height / 2, halfLength, yaw, true)
                }

                MeleeHitboxType.CAPSULE -> {
                    val startOffset = hitbox.zFrom
                    val endOffset = hitbox.zFrom + if (hitbox.range > 0) hitbox.range else context.reach
                    val center = context.eyePos.add(0.0, hitbox.yOffset, 0.0).add(look.scale((startOffset + endOffset) / 2))
                    DebugBox(center, hitbox.radius, hitbox.radius, (endOffset - startOffset) / 2, yaw, false)
                }
            }
        }
    }

    private const val EPSILON = 1.0E-8
    private const val SEGMENT_SAMPLES = 8
    private const val SUBDIVISION_SAMPLES = 8

    /** 打头判定相对 `eyeHeight` 的下容差 */
    const val HEADSHOT_MARGIN_BELOW = 0.25

    /** 打头判定相对 `eyeHeight` 的上容差 */
    const val HEADSHOT_MARGIN_ABOVE = 0.3

    /** 打腿判定的身高比例 */
    const val LEGSHOT_RATIO = 0.33

    /** 默认动作（没配 `MeleeActions` 时） */
    val DEFAULT_ACTION: MeleeAction = MeleeAction()
}
