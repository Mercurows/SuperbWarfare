package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.data.gun.melee.*
import com.atsuishio.superbwarfare.tools.MeleeQuery.LOS_EPSILON
import com.atsuishio.superbwarfare.tools.MeleeQuery.SEGMENT_SAMPLES
import com.atsuishio.superbwarfare.tools.MeleeQuery.boxHit
import com.atsuishio.superbwarfare.tools.MeleeQuery.capsuleHit
import com.atsuishio.superbwarfare.tools.MeleeQuery.resolve
import com.atsuishio.superbwarfare.tools.MeleeQuery.resolveDiag
import com.atsuishio.superbwarfare.tools.MeleeQuery.yawPitchQuaternion
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
     * 一次判定里**每个候选实体**的处置结果，只给调试日志用。
     *
     * 排查"盒子明明罩住了却打不到"时，光看命中列表是没用的 ——
     * 必须能看到候选是谁、卡在哪一步（形状不相交 / 被方块挡住）。
     */
    enum class RejectReason(val label: String) {
        /** 命中了 */
        HIT("hit"),

        /** 粗筛就没找到（不会出现在这里，只用于日志对照） */
        NOT_COARSE_FILTERED("not-coarse-filtered"),

        /** 形状不相交：角度/距离/盒体尺寸不满足 */
        SHAPE("shape"),

        /** 形状相交了，但 `Occlusion` 判定视线被方块挡住 */
        OCCLUSION("occlusion"),
    }

    data class CandidateDiag(
        val entity: Entity,
        val reason: RejectReason,
        val distance: Double,
        val angle: Double,
        /** 枚举里的 `Cone` 水平夹角（度）；Box/Capsule 恒为 0 */
        val yawDelta: Double = 0.0,
        /** 枚举里的 `Cone` 垂直夹角（度）；Box/Capsule 恒为 0 */
        val pitchDelta: Double = 0.0,
        val hitPos: Vec3? = null,
    )

    /** [resolveDiag] 的返回：命中 + 每个候选的处置结果 */
    data class DiagResult(
        val hits: List<Hit>,
        val candidates: List<CandidateDiag>,
    )

    /**
     * 与 [resolve] 完全同一条路径，额外收集每个候选的处置结果。
     *
     * 只在开了 `melee_debug_log` 时由调用方使用，正常路径仍然走 [resolve]（不产生这份开销）。
     */
    @JvmStatic
    @JvmOverloads
    fun resolveDiag(
        level: Level,
        attacker: Entity,
        action: ResolvedMeleeAction,
        context: Context? = null,
        candidates: List<Entity>? = null,
    ): DiagResult {
        val diags = ArrayList<CandidateDiag>()
        val hits = resolve(level, attacker, action, context, candidates, diags)
        return DiagResult(hits, diags)
    }

    /**
     * 完整判定：粗筛 → 逐个形状精判 → 排序 → 截断 → 命中区域。
     *
     * @param candidates 粗筛结果；传 `null` 时由本方法按 [Context.reach] 自行粗筛
     * @param diagnostics 非空时收集每个候选的处置结果（调试用）
     */
    @JvmStatic
    @JvmOverloads
    fun resolve(
        level: Level,
        attacker: Entity,
        action: ResolvedMeleeAction,
        context: Context? = null,
        candidates: List<Entity>? = null,
        diagnostics: MutableList<CandidateDiag>? = null,
    ): List<Hit> {
        val player = attacker as? Player ?: return emptyList()
        val ctx = context ?: contextOf(player, action)

        val entityList = candidates ?: coarseFilter(level, attacker, ctx.reach, action = action)

        val hitbox = action.hitbox
        val offsets = action.sweep?.sampleOffsets() ?: listOf(0.0)
        val byEntity = LinkedHashMap<Entity, Hit>()
        val diagByEntity = LinkedHashMap<Entity, CandidateDiag>()

        for ((sampleIndex, offset) in offsets.withIndex()) {
            val yaw = (ctx.yaw + offset).toFloat()
            val look = lookVector(yaw, ctx.pitch)

            for (entity in entityList) {
                if (byEntity.containsKey(entity)) continue
                val box = entity.boundingBox
                if (box.xsize <= 0.0 || box.ysize <= 0.0 || box.zsize <= 0.0) continue

                // 形状判定：为诊断顺手把圆锥的两个夹角也算出来（只在诊断开启时算）
                val shape = when (hitbox.type) {
                    MeleeHitboxType.CONE -> coneHit(ctx.eyePos, yaw, ctx.pitch, box, hitbox, ctx.reach, diagnostics != null)
                    MeleeHitboxType.BOX -> ShapeHit(boxHit(ctx.eyePos, yaw, ctx.pitch, look, box, hitbox))
                    MeleeHitboxType.CAPSULE -> ShapeHit(capsuleHit(ctx.eyePos, yaw, ctx.pitch, look, box, hitbox, ctx.reach))
                }

                val delta = ctx.eyePos.vectorTo(shape.point ?: closestPointInBox(box, ctx.eyePos))
                val distance = delta.length()
                val angle = angleBetween(lookVector(yaw, ctx.pitch), delta)

                if (shape.point == null) {
                    if (diagnostics != null && !diagByEntity.containsKey(entity)) {
                        diagByEntity[entity] = CandidateDiag(
                            entity, RejectReason.SHAPE, distance, angle,
                            shape.yawDelta, shape.pitchDelta,
                        )
                    }
                    continue
                }

                val hitPos = shape.point
                if (hitbox.occlusion && !hasLineOfSight(level, attacker, ctx.eyePos, hitPos, box)) {
                    if (diagnostics != null) {
                        diagByEntity[entity] = CandidateDiag(
                            entity, RejectReason.OCCLUSION, distance, angle,
                            shape.yawDelta, shape.pitchDelta, hitPos,
                        )
                    }
                    continue
                }

                byEntity[entity] = Hit(
                    entity = entity,
                    hitPos = hitPos,
                    distance = distance,
                    angle = angle,
                    sampleIndex = sampleIndex,
                    order = 0,
                    headshot = isHeadshot(entity, hitPos),
                    legshot = isLegshot(entity, hitPos),
                )
                if (diagnostics != null) {
                    diagByEntity[entity] = CandidateDiag(
                        entity, RejectReason.HIT, distance, angle,
                        shape.yawDelta, shape.pitchDelta, hitPos,
                    )
                }
            }
        }

        val sorted = sort(byEntity.values, action.sortBy)
        val limited = if (action.maxTargets > 0) sorted.take(action.maxTargets) else sorted

        if (diagnostics != null) {
            // 被 MaxTargets 截掉的也算"命中"，保留原始判定结论便于排查
            diagnostics += diagByEntity.values
        }

        return limited.mapIndexed { index, hit -> hit.copy(order = index) }
    }

    /**
     * 粗筛：以玩家为原点、`reach + 形状外扩 + 扫掠外接半径` 构造 AABB。
     *
     * 过滤器沿用 [SeekTool.BASIC_FILTER] + `NOT_IN_SMOKE` + 同队排除，并排除自己骑的载具。
     *
     * **外扩必须覆盖形状自身的尺寸**：`Cone`/`Capsule` 只吃 [ResolvedMeleeAction.hitbox] 的
     * `range`，但 `Box` 用的是 `length`（`range` 不参与判定）。之前粗筛只按 `reach` 画球，
     * `"Length"` 写得比 `reach` 大时（例如 `Length: 8, Range: 0`）盒子前段的目标会被粗筛直接
     * 漏掉 —— 表现为"盒子明明罩住了却打不到"。
     */
    @JvmStatic
    @JvmOverloads
    fun coarseFilter(
        level: Level,
        attacker: Entity,
        reach: Double,
        sweep: MeleeSweep? = null,
        action: ResolvedMeleeAction? = null,
    ): List<Entity> {
        val hitbox = action?.hitbox
        val shapeExtent = when (hitbox?.type) {
            // Box 沿视线伸出：从 ZFrom 到 ZFrom + Length
            MeleeHitboxType.BOX -> max(hitbox.zFrom + hitbox.length, 0.0).coerceAtLeast(reach)
            // Capsule 同理，但它用的是 range（已被 reach 包含）
            MeleeHitboxType.CAPSULE -> reach
            else -> reach
        }
        val lateral = sweep?.maxLateral(shapeExtent) ?: 0.0
        val radius = shapeExtent + lateral + 1.0
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
     * 形状判定的结果。
     *
     * @param point 入射点（判定体 ∩ 目标 AABB 的最近点）；`null` = 不相交
     * @param yawDelta 仅 `Cone`：目标方向与视线的**水平**夹角（度）
     * @param pitchDelta 仅 `Cone`：目标方向与视线的**垂直**夹角（度）
     */
    private data class ShapeHit(
        val point: Vec3?,
        val yawDelta: Double = 0.0,
        val pitchDelta: Double = 0.0,
    )

    /** 便利构造：只有点、没有角度信息（Box / Capsule） */
    private fun ShapeHit(point: Vec3?) = ShapeHit(point, 0.0, 0.0)

    /**
     * 圆锥（兼容旧行为）：`|Δyaw| ≤ Angle/2`、`|Δpitch| ≤ Pitch/2`、距离 ≤ [MeleeHitbox.range]。
     *
     * 夹角与距离都量到**目标 AABB 的最近点**（`closestPointInBox`）。
     * 注意这个点**不是脚底**：它把眼睛夹进 AABB 里，所以站着打站着时 y 会被夹到**眼睛高度**，
     * 只有俯仰差距大到让最近点落到 box 的顶/底面时才会带垂直分量。
     * 换句话说"用最近点算俯仰角"本身没问题（实测：平视 0°、俯视 30° 打胸口都在容差内），
     * 旧实现的「眼→眼」只是同一件事的近似。
     */
    private fun coneHit(
        eyePos: Vec3,
        yaw: Float,
        pitch: Float,
        box: AABB,
        hitbox: MeleeHitbox,
        reach: Double,
        withAngles: Boolean = false,
    ): ShapeHit {
        val point = closestPointInBox(box, eyePos)
        val delta = eyePos.vectorTo(point)
        if (delta.lengthSqr() > reach * reach) return ShapeHit(null)
        if (delta.lengthSqr() < EPSILON) return ShapeHit(box.center)

        val horizontal = sqrt(delta.x * delta.x + delta.z * delta.z)
        val targetYaw = Math.toDegrees(atan2(-delta.x, delta.z)).toFloat()
        val targetPitch = Math.toDegrees(asin((delta.y / delta.length()).coerceIn(-1.0, 1.0))).toFloat()

        val yawDelta = abs(Mth.wrapDegrees(targetYaw - yaw)).toDouble()
        val pitchDelta = abs(Mth.wrapDegrees(targetPitch - pitch)).toDouble()
        val reportYaw = if (withAngles) yawDelta else 0.0
        val reportPitch = if (withAngles) pitchDelta else 0.0

        if (yawDelta > hitbox.angle / 2.0) return ShapeHit(null, reportYaw, reportPitch)
        if (horizontal > EPSILON && pitchDelta > hitbox.pitch / 2.0) {
            return ShapeHit(null, reportYaw, reportPitch)
        }

        return ShapeHit(point, reportYaw, reportPitch)
    }

    /**
     * 盒体：OBB ∩ 目标 AABB。
     *
     * 盒体**跟着视线转**（yaw + pitch，见 [yawPitchQuaternion]）：
     * 中心 = 眼睛 + 局部上偏移(`YOffset`) + 视线方向 × (`ZFrom` + `Length/2`)，
     * 半长 = (`Width/2`, `Height/2`, `Length/2`)，局部 +Z 指向视线。
     *
     * 直接用 [OBB] + [OBB.isColliding]，与载具碰撞判定同一套 SAT 实现。
     * 盒体自带明确尺寸（`Width`/`Height`/`Length`），所以**不再额外用 `reach` 收口**——
     * 否则 `range` 一写大就会把盒体判定放大成"看不见的远程攻击"。
     */
    private fun boxHit(
        eyePos: Vec3,
        yaw: Float,
        pitch: Float,
        look: Vec3,
        box: AABB,
        hitbox: MeleeHitbox,
    ): Vec3? {
        val halfLength = max(hitbox.length, 0.0) / 2.0
        val center = eyePos
            .add(localUpOffset(yaw, pitch, hitbox.yOffset))
            .add(look.scale(hitbox.zFrom + halfLength))

        val obb = OBB(
            Vector3d(center.x, center.y, center.z),
            Vector3d(max(hitbox.width, 0.0) / 2.0, max(hitbox.height, 0.0) / 2.0, halfLength),
            yawPitchQuaternion(yaw, pitch),
            OBB.Part.EMPTY,
        )

        if (!OBB.isColliding(obb, box)) return null

        return closestPointInBox(box, eyePos)
    }

    /**
     * 胶囊：线段（沿视线 `zFrom → zFrom + range`）到目标 AABB 的最近距离 ≤ `Radius`。
     *
     * 线段与 [MeleeHitbox.yOffset] 都跟着视线转（同 [boxHit]）。
     */
    private fun capsuleHit(
        eyePos: Vec3,
        yaw: Float,
        pitch: Float,
        look: Vec3,
        box: AABB,
        hitbox: MeleeHitbox,
        reach: Double,
    ): Vec3? {
        val startOffset = hitbox.zFrom
        val endOffset = hitbox.zFrom + if (hitbox.range > 0) hitbox.range else reach
        val origin = eyePos.add(localUpOffset(yaw, pitch, hitbox.yOffset))
        val start = origin.add(look.scale(startOffset))
        val end = origin.add(look.scale(endOffset))

        val radius = max(hitbox.radius, 0.0)
        val (segmentPoint, boxPoint) = closestSegmentToBox(start, end, box)
        if (segmentPoint.distanceTo(boxPoint) > radius) return null

        return boxPoint
    }

    // ------------------------------------------------------------------ 几何工具

    /**
     * 把 MC 的 `yaw` + `pitch` 变成"局部坐标 → 世界坐标"的四元数：
     *
     * ```
     * Quaterniond().rotateY(-yaw).rotateX(+pitch)
     * ```
     *
     * 变换后的三根轴：
     * - 局部 **+Z**（`Length`/`Range`/`ZFrom` 的方向）→ **视线** `(-sin y·cos p, -sin p, cos y·cos p)`
     * - 局部 **+X**（`Width` 的方向）→ 世界水平右方 `(cos y, 0, sin y)`，**与 pitch 无关**
     * - 局部 **+Y**（`Height` 的方向）→ `look × right`，即与视线垂直的"上"
     *
     * **两个符号都是必须的**（都用 JOML 1.10.5 实测过 yaw ∈ {0, ±45, 90, 135, 180}、
     * pitch ∈ {-60, -45, -30, 0, 10, 20, 30, 45, 89, 90}）：
     *
     * 1. **`-yaw` 不能写成 `+yaw`**：MC 的 yaw 是**从 +Z 朝 +X** 增加的
     *    （yaw 90° 看向 **-X**），而 JOML 的 `rotateY(θ)` 是右手系绕 +Y，把局部 +Z 转到
     *    `(sin θ, 0, cos θ)`（θ=90° 指向 **+X**）——两者**手性相反**，写 `+yaw` 会让盒子与朝向
     *    正好差 180°（yaw 0/180 时看不出来，45°/90° 最明显）。
     * 2. **`+pitch` 不能写成 `-pitch`**：MC 的 pitch **向下为正**，而 JOML 的 `rotateX(θ)`
     *    把局部 +Z 转到 `(0, -sin θ, cos θ)`（θ>0 朝 **-Y** = 朝下）——所以正好是 `+pitch`。
     *
     * 仓库里的同类转换也都是这个符号，可以对照：`VectorTool.combineRotationsYaw`、
     * `VehicleMotionUtils` / `VehicleVecUtils` 的 `Axis.YP.rotationDegrees(-vehicle.yRot)`、
     * `CameraMixin`、`C4Entity` 的 `.rotateY(-yaw)`。
     *
     * 之所以单独抽一个函数：判定（[boxHit] / [capsuleHit]）与调试渲染（`MeleeDebugRenderer`）
     * 必须用**同一个**旋转，否则"看到的盒子"和"判定的盒子"会不一致——这两个符号各写反过一次。
     */
    @JvmStatic
    fun yawPitchQuaternion(yaw: Float, pitch: Float): Quaterniond =
        Quaterniond()
            .rotateY(Math.toRadians(-yaw.toDouble()))
            .rotateX(Math.toRadians(pitch.toDouble()))

    /**
     * 把 [MeleeHitbox.yOffset]（相对眼睛的垂直偏移）按当前朝向转成世界向量。
     *
     * 旧实现是直接 `.add(0, yOffset, 0)`（写死世界 Y）——抬头看天时"向下偏 0.4"的盒体
     * 会横在头顶而不是贴在视线下方。现在沿 `look × right` 偏移，跟视角一起转。
     */
    @JvmStatic
    fun localUpOffset(yaw: Float, pitch: Float, offset: Double): Vec3 {
        if (offset == 0.0) return Vec3.ZERO
        val up = Vector3d(0.0, 1.0, 0.0)
        yawPitchQuaternion(yaw, pitch).transform(up)
        return Vec3(up.x * offset, up.y * offset, up.z * offset)
    }

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

    /**
     * 视线是否通畅（用方块 COLLIDER 射线，不含流体）。
     *
     * 近战的距离通常只有 1~4 格，所以"贴在方块上"比"隔着墙"常见得多，两个坑必须绕开：
     *
     * 1. **射线起点不能落在方块里**：把眼睛到目标最近点这条射线**按攻击者的碰撞箱修一下**，
     *    起点推到自身碰撞箱之外。玩家/怪紧贴在一起时，目标 AABB 的最近点会落在攻击者自己的
     *    碰撞箱内，而 `level.clip` 的起点只要在方块内就会立刻返回 BLOCK → 贴脸砍永远打不中。
     * 2. **终点不能落在目标表面之外**：入射点正好在目标 AABB 的面上，射线在那里截断即可，
     *    不需要再往外探 —— 用 `to - dir * 1e-4` 收尾，避免浮点误差把终点推到目标背后的方块里。
     *
     * @param targetBox 目标 AABB；非空时用它的最近点作为终点
     */
    private fun hasLineOfSight(
        level: Level,
        attacker: Entity,
        from: Vec3,
        to: Vec3,
        targetBox: AABB? = null,
    ): Boolean {
        val end = targetBox?.let { closestPointInBox(it, from) } ?: to
        val raw = end.subtract(from)
        if (raw.lengthSqr() < EPSILON) return true

        val direction = raw.normalize()
        val start = pushOutside(from, direction, attacker.boundingBox)
        val stop = end.subtract(direction.scale(LOS_EPSILON))
        if (start.distanceToSqr(stop) < EPSILON) return true

        val result = level.clip(
            ClipContext(start, stop, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, attacker)
        )
        return result.type != HitResult.Type.BLOCK
    }

    /**
     * 把 [point] 沿 [direction] 推到 [box] 之外（`box` 为自身碰撞箱）。
     *
     * 点在箱内时，取"沿 direction 到各面距离的最小正值"再多推 [LOS_EPSILON]；
     * 点在箱外时原样返回。
     */
    private fun pushOutside(point: Vec3, direction: Vec3, box: AABB): Vec3 {
        if (!box.contains(point)) return point

        var t = Double.MAX_VALUE
        if (direction.x > EPSILON) t = min(t, (box.maxX - point.x) / direction.x)
        if (direction.x < -EPSILON) t = min(t, (box.minX - point.x) / direction.x)
        if (direction.y > EPSILON) t = min(t, (box.maxY - point.y) / direction.y)
        if (direction.y < -EPSILON) t = min(t, (box.minY - point.y) / direction.y)
        if (direction.z > EPSILON) t = min(t, (box.maxZ - point.z) / direction.z)
        if (direction.z < -EPSILON) t = min(t, (box.minZ - point.z) / direction.z)

        if (t == Double.MAX_VALUE || t < 0.0) return point
        return point.add(direction.scale(t + LOS_EPSILON))
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
     * 调试线框用的形状描述。
     *
     * **必须能如实画出三种形状**，不能再"统一成盒体近似"——那个近似盒长度只有 `reach/2`、
     * 宽度却是 `reach×sin(半角)`，画出来又短又粗，于是"盒子里的怪打不到、盒子外的怪反而挨打"
     * 这种观感全是这一个假盒子造成的（见 §11.2-㉓）。这里改成：
     *
     * - [Cone]：顶点 + 末端圆环（真实 `reach`、水平半角、垂直半角），
     *   垂直半角 ≥ 90° 时只受水平角限制，画成一个球面天线罩
     * - [Box]：带 yaw/pitch 姿态的盒体
     * - [Segment]：线段的胶囊（画成两点之间的线，渲染侧可加端盖）
     */
    sealed interface DebugShape {
        /** 参数：`(start, end, radius)` */
        data class Segment(val start: Vec3, val end: Vec3, val radius: Double) : DebugShape

        /** 参数：`(apex, axis, reach, radius, verticalCapped)` */
        data class Cone(
            val apex: Vec3,
            val axis: Vec3,
            val reach: Double,
            val radius: Double,
            /** `true` = 画成圆环 + 母线；`false` = 垂直不受限，只画球面天线罩 */
            val verticalCapped: Boolean,
        ) : DebugShape

        /** 参数：`(center, radiusX, radiusY, radiusZ, yaw, pitch)` */
        data class Box(
            val center: Vec3,
            val radiusX: Double,
            val radiusY: Double,
            val radiusZ: Double,
            val yaw: Float,
            val pitch: Float,
        ) : DebugShape
    }

    @JvmStatic
    fun debugShapes(context: Context, action: ResolvedMeleeAction): List<DebugShape> {
        val hitbox = action.hitbox
        val offsets = action.sweep?.sampleOffsets() ?: listOf(0.0)

        return offsets.map { offset ->
            val yaw = (context.yaw + offset).toFloat()
            val pitch = context.pitch
            val look = lookVector(yaw, pitch)
            when (hitbox.type) {
                MeleeHitboxType.CONE -> {
                    val halfAngle = Math.toRadians((hitbox.angle / 2).coerceIn(0.0, 89.9))
                    DebugShape.Cone(
                        apex = context.eyePos,
                        axis = look,
                        reach = context.reach,
                        radius = context.reach * sin(halfAngle),
                        verticalCapped = hitbox.pitch < 180.0,
                    )
                }

                MeleeHitboxType.BOX -> {
                    val halfLength = max(hitbox.length, 0.0) / 2.0
                    val center = context.eyePos
                        .add(localUpOffset(yaw, pitch, hitbox.yOffset))
                        .add(look.scale(hitbox.zFrom + halfLength))
                    DebugShape.Box(
                        center, hitbox.width / 2, hitbox.height / 2, halfLength, yaw, pitch,
                    )
                }

                MeleeHitboxType.CAPSULE -> {
                    val startOffset = hitbox.zFrom
                    val endOffset = hitbox.zFrom + if (hitbox.range > 0) hitbox.range else context.reach
                    val origin = context.eyePos.add(localUpOffset(yaw, pitch, hitbox.yOffset))
                    DebugShape.Segment(
                        start = origin.add(look.scale(startOffset)),
                        end = origin.add(look.scale(endOffset)),
                        radius = max(hitbox.radius, 0.0),
                    )
                }
            }
        }
    }

    /** 末端圆环的采样点数（调试线框） */
    const val CONE_RING_SEGMENTS: Int = 32

    private const val EPSILON = 1.0E-8

    /** 视线射线的收尾余量：起点外推 / 终点内收用 */
    private const val LOS_EPSILON = 1.0E-4
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
