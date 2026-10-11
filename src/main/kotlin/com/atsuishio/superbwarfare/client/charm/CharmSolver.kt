package com.atsuishio.superbwarfare.client.charm

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.*

class CharmSolver {

    /** 质点位置（**视图空间**，相对相机），方块 */
    private val bob = Vector3f()

    /** 上一步的 [bob] */
    private val bobPrev = Vector3f()

    /** 本步的悬挂点（视图空间，相对相机） */
    private val anchor = Vector3f()

    /** 上一步的悬挂点：携带项与跳变检测都要读它 */
    private val anchorPrev = Vector3f()

    /** 重力方向（单位向量，视图空间，指向下） */
    private val gravityDir = Vector3f(0f, -1f, 0f)

    /** 本帧 [CharmFrame.sideAxis] 的副本：软限位要用的"横向"参考轴（模型局部 +X 在视图空间的方向） */
    private val sideAxis = Vector3f(1f, 0f, 0f)

    /** 参考系内的绳向量（单位向量，指向挂件） */
    val rope = Vector3f(0f, -1f, 0f)

    /** 静止余摆的相位（秒） */
    private var swayTime = 0f

    private var initialized = false

    /** 是否已经有过一次有效推进 */
    val isReady: Boolean get() = initialized

    // 逐帧复用的草稿，避免每步分配
    private val scratch = Vector3f()
    private val pseudo = Vector3f()
    private val omega = Vector3f()
    private val alpha = Vector3f()
    private val velocity = Vector3f()
    private val accel = Vector3f()

    /**
     * 把质点直接放到"悬挂点沿重力方向下方"，并清空全部速度。
     *
     * [gravityIn] 传的是**视图空间**的重力（含大小），只取它的方向；朝向被相机抬起来时
     * 静止方向也随之偏，归位要用同一套方向，否则刚切回第一人称会先歪一下再荡正。
     */
    fun reset(anchorIn: Vector3f, gravityIn: Vector3f, length: Float) {
        anchor.set(anchorIn)
        anchorPrev.set(anchorIn)
        gravityDir.set(gravityIn)
        if (gravityDir.lengthSquared() < EPSILON) {
            gravityDir.set(0f, -1f, 0f)
        } else {
            gravityDir.normalize()
        }
        bob.set(
            anchorIn.x + gravityDir.x * length,
            anchorIn.y + gravityDir.y * length,
            anchorIn.z + gravityDir.z * length,
        )
        bobPrev.set(bob)
        rope.set(gravityDir)
        swayTime = 0f
        initialized = true
    }

    /** 坐标系换了（换手/换枪/长时间没渲染）时用它，下一帧 [update] 会重新归位 */
    fun invalidate() {
        initialized = false
    }

    /**
     * 推进一个物理子步。
     *
     * @param anchorIn 悬挂点在**视图空间**里的位置（方块），相对相机。
     * @param frame 本帧的相机运动量，**一帧算一次、各个子步共用**（见 [CharmFrame] 的说明）。
     * @param dt 子步时长（秒），调用方传固定值。
     * @return `true` = 这一步真的推进了；`false` = 只是（重新）归位，画绑定姿态即可。
     */
    fun update(anchorIn: Vector3f, frame: CharmFrame, p: CharmParams, dt: Float): Boolean {
        gravityDir.set(frame.gravity)
        if (gravityDir.lengthSquared() < EPSILON) {
            gravityDir.set(0f, -1f, 0f)
        } else {
            gravityDir.normalize()
        }
        sideAxis.set(frame.sideAxis)
        if (sideAxis.lengthSquared() < EPSILON) {
            sideAxis.set(1f, 0f, 0f)
        } else {
            sideAxis.normalize()
        }

        if (!initialized) {
            reset(anchorIn, frame.gravity, p.length)
            return false
        }

        // 悬挂点单步跳变过大（换枪、绘制动画、切副武器……）：直接归位。
        // 不这么做的话，那一下位移会在球面约束里被折算成一个巨大的切向速度，甩得像弹弓。
        if (anchorPrev.distanceSquared(anchorIn) > MAX_ANCHOR_JUMP * MAX_ANCHOR_JUMP) {
            reset(anchorIn, frame.gravity, p.length)
            return false
        }

        anchor.set(anchorIn)

        // 质点与悬挂点各自的帧内位移（blocks/step）
        val dRx = bob.x - bobPrev.x
        val dRy = bob.y - bobPrev.y
        val dRz = bob.z - bobPrev.z
        val dAx = anchor.x - anchorPrev.x
        val dAy = anchor.y - anchorPrev.y
        val dAz = anchor.z - anchorPrev.z

        // 伪力（视图空间）：离心 + 欧拉 + 科里奥利。
        omega.set(frame.omega)
        alpha.set(frame.alpha)

        // 质点相对相机的速度（视图空间），只用来算科里奥利
        velocity.set(dRx, dRy, dRz).mul(1f / dt)

        // (Ω×p)×Ω = -Ω×(Ω×p) ⇒ 离心力：把质点往外推
        pseudo.set(scratch.set(omega).cross(bob).cross(omega))
        // p×Ω̇ = -Ω̇×p ⇒ 欧拉力：角加速度那一下的切向滞后/甩出
        pseudo.add(scratch.set(bob).cross(alpha))
        // 2(v×Ω) = -2Ω×v ⇒ 科里奥利力
        pseudo.add(scratch.set(velocity).cross(omega).mul(2f))

        // ⚠ 缩放的是**合力**，不是 Ω。
        // 缩放 Ω 看着更"物理"（等于假装转得慢一点），但离心力正比于 Ω²，于是摆角变成
        // `turnResponse² · ω² · R / g` —— 旋钮拧到 0.3 实际只有 9% 的力度，写 0.3 得到的
        // 是 0.3² 的效果，数据和文档差一个数量级。更糟的是三股力的**比例**也被改掉了：
        // 欧拉力/科里奥利力只正比于 Ω，会相对离心力强出 1/turnResponse 倍，
        // 起转/停转那一下的切向甩动盖过匀速时的离心飘。
        // 缩合力则三股力同比例缩，摆角就是老老实实的 `turnResponse · ω² · R / g`。
        pseudo.mul(p.turnResponse)

        val damp = exp(-p.damping * dt)
        val carry = 1f - p.response
        val dt2 = dt * dt
        val g = p.gravity

        // 本步的总加速度：重力 + 相机惯性 + 转身伪力
        accel.set(
            gravityDir.x * g - p.response * frame.camAccel.x + pseudo.x,
            gravityDir.y * g - p.response * frame.camAccel.y + pseudo.y,
            gravityDir.z * g - p.response * frame.camAccel.z + pseudo.z,
        )

        // ⚠ 先把加速度的**径向**分量扣掉，再自由飞
        val nx = bob.x - anchor.x
        val ny = bob.y - anchor.y
        val nz = bob.z - anchor.z
        val nLen = sqrt(nx * nx + ny * ny + nz * nz)
        // 本步的摆角上限（按偏离方位定，见 [applySoftLimit]）；退化时退回枪身侧的值
        var limit = p.maxAngle
        if (nLen > EPSILON) {
            val inv = 1f / nLen
            val ux = nx * inv
            val uy = ny * inv
            val uz = nz * inv
            val radial = accel.x * ux + accel.y * uy + accel.z * uz
            accel.x -= ux * radial
            accel.y -= uy * radial
            accel.z -= uz * radial

            // ⚠ 软限位必须在这之后（它已经是纯切向的），且在伪力的 turnResponse 缩放之后 ——
            // 它是回正力，不是伪力，不参与那个系数。
            limit = applySoftLimit(p, ux, uy, uz)
        }

        bobPrev.set(bob)
        // ⚠ 悬挂点的上一步位置也必须推进：携带项、科里奥利的基线和跳变检测都读它，
        // 忘了写就会拿一个越来越旧的锚点去算 —— 携带整项失效，跳变检测还会误报。
        anchorPrev.set(anchor)

        bob.set(
            bob.x + dRx * damp + dAx * carry + accel.x * dt2,
            bob.y + dRy * damp + dAy * carry + accel.y * dt2,
            bob.z + dRz * damp + dAz * carry + accel.z * dt2,
        )

        projectToRope(p)
        clampSwingAngle(p, limit)
        buildRope()
        applyIdleSway(p, dt)

        return true
    }

    private fun swingLimit(p: CharmParams, nx: Float, ny: Float, nz: Float): Float {
        if (p.blockedAxisSign == 0f || p.maxAngleFree <= p.maxAngle) return p.maxAngle

        // 偏离方向 = 绳方向**自己的**横向分量（n̂ 扣掉沿 ĝ 的分量），模长 sinθ
        //
        // ⚠ 不能拿重力的横向分量 `ĝ - n̂(n̂·ĝ)` 来当这个用：那个向量是"回正方向"，
        // 它在 [sideAxis] 上的投影是 `cosθ` 而不是 ±1 —— 越偏离越小，55° 时只剩一半，
        // 于是"哪一侧"会随着摆角模糊掉（实测表现为摆角在 70° 附近被莫名收住）。
        val cos = nx * gravityDir.x + ny * gravityDir.y + nz * gravityDir.z
        val tx = nx - gravityDir.x * cos
        val ty = ny - gravityDir.y * cos
        val tz = nz - gravityDir.z * cos
        val tLen = sqrt(tx * tx + ty * ty + tz * tz)
        // θ ≈ 0（或 ≈180°）：方位角没有意义，但那里上限也不起作用
        if (tLen < EPSILON) return p.maxAngle

        val sideComp = (tx * sideAxis.x + ty * sideAxis.y + tz * sideAxis.z) / tLen
        // 单边：只有横向分量落在枪身侧（w > 0）才从 maxAngleFree 往 maxAngle 收，
        // 落在正前/正后（≈0）或另一侧（< 0）时 w 削到 0，一律取最宽的 maxAngleFree
        val w = (p.blockedAxisSign * sideComp).coerceIn(0f, 1f)
        return p.maxAngleFree + (p.maxAngle - p.maxAngleFree) * w
    }

    private fun applySoftLimit(p: CharmParams, nx: Float, ny: Float, nz: Float): Float {
        val limit = swingLimit(p, nx, ny, nz)
        val k = p.limitStiffness
        if (k <= 0f) return limit

        val cos = nx * gravityDir.x + ny * gravityDir.y + nz * gravityDir.z
        val dx = gravityDir.x - nx * cos
        val dy = gravityDir.y - ny * cos
        val dz = gravityDir.z - nz * cos
        if (dx * dx + dy * dy + dz * dz < EPSILON) return limit

        val r = (acos(cos.coerceIn(-1f, 1f)) / limit).coerceIn(0f, 1f)
        val scale = p.gravity * k * r * r
        accel.x += dx * scale
        accel.y += dy * scale
        accel.z += dz * scale
        return limit
    }

    /** 球面约束：把质点拉回"以悬挂点为球心、摆长为半径"的球面上 */
    private fun projectToRope(p: CharmParams) {
        val ox = bob.x - anchor.x
        val oy = bob.y - anchor.y
        val oz = bob.z - anchor.z
        val distSq = ox * ox + oy * oy + oz * oz

        if (distSq < EPSILON) {
            // 退化：质点正好落在悬挂点上，沿重力方向给它一个位置
            bob.set(
                anchor.x + gravityDir.x * p.length,
                anchor.y + gravityDir.y * p.length,
                anchor.z + gravityDir.z * p.length,
            )
            return
        }

        val k = p.length / sqrt(distSq)
        bob.set(anchor.x + ox * k, anchor.y + oy * k, anchor.z + oz * k)
    }

    private fun clampSwingAngle(p: CharmParams, limit: Float) {
        val ox = bob.x - anchor.x
        val oy = bob.y - anchor.y
        val oz = bob.z - anchor.z
        val len = sqrt(ox * ox + oy * oy + oz * oz)
        if (len < EPSILON) return

        val nx = ox / len
        val ny = oy / len
        val nz = oz / len

        // 与重力方向的夹角余弦
        val cos = nx * gravityDir.x + ny * gravityDir.y + nz * gravityDir.z
        if (cos >= cos(limit)) return

        // 旋转轴 = n × ĝ
        var ax = ny * gravityDir.z - nz * gravityDir.y
        var ay = nz * gravityDir.x - nx * gravityDir.z
        var az = nx * gravityDir.y - ny * gravityDir.x
        val axisLen = sqrt(ax * ax + ay * ay + az * az)
        if (axisLen < EPSILON) {
            // 方向几乎正对重力反方向：轴任取一个与重力垂直的方向
            ax = 0f
            ay = gravityDir.z
            az = -gravityDir.y
            val l = sqrt(ax * ax + ay * ay + az * az)
            if (l < EPSILON) {
                ax = 1f
                ay = 0f
                az = 0f
            } else {
                ax /= l
                ay /= l
                az /= l
            }
        } else {
            ax /= axisLen
            ay /= axisLen
            az /= axisLen
        }

        val excess = acos(cos.coerceIn(-1f, 1f)) - limit
        if (excess <= 0f) return

        val q = Quaternionf().fromAxisAngleRad(ax, ay, az, excess)
        val dir = q.transform(Vector3f(nx, ny, nz))
        bob.set(anchor.x + dir.x * p.length, anchor.y + dir.y * p.length, anchor.z + dir.z * p.length)
    }

    /** 由质点位置得到绳向量（单位向量，指向挂件） */
    private fun buildRope() {
        rope.set(bob.x - anchor.x, bob.y - anchor.y, bob.z - anchor.z)
        if (rope.lengthSquared() < EPSILON) {
            rope.set(gravityDir)
        } else {
            rope.normalize()
        }
    }

    /**
     * 站立不动时的余摆：**只加在输出方向上、不写回状态**，所以它不会反过来影响物理。
     *
     * 幅度只有零点几度，作用是让"完全静止"看起来不像卡住。
     */
    private fun applyIdleSway(p: CharmParams, dt: Float) {
        if (p.idleSway <= 0f) return

        swayTime += dt
        val x = sin(swayTime * SWAY_SPEED_X) * p.idleSway
        val z = sin(swayTime * SWAY_SPEED_Z + SWAY_PHASE_Z) * p.idleSway
        val q = Quaternionf()
            .fromAxisAngleRad(1f, 0f, 0f, x)
            .mul(Quaternionf().fromAxisAngleRad(0f, 0f, 1f, z))
        q.transform(rope)
    }

    companion object {
        private const val EPSILON = 1.0e-9f

        /** 单步悬挂点位移超过这个距离（方块）就判定为不连续 */
        private const val MAX_ANCHOR_JUMP = 0.6f

        private const val SWAY_SPEED_X = 0.9f
        private const val SWAY_SPEED_Z = 0.63f
        private const val SWAY_PHASE_Z = 1.3f
    }
}

/**
 * 一帧的解算输入，**全部在视图空间**（相机运动量带符号、按秒计）
 */
class CharmFrame {
    /**
     * 重力（视图空间，**含大小**，方块/秒²）
     */
    @JvmField
    val gravity: Vector3f = Vector3f(0f, -9.8f, 0f)

    /**
     * **附件模型局部 +X 在视图空间里的方向**（单位向量）
     */
    @JvmField
    val sideAxis: Vector3f = Vector3f(1f, 0f, 0f)

    /** 相机线加速度（视图空间，方块/秒²），来自相机世界坐标的差分 */
    @JvmField
    val camAccel: Vector3f = Vector3f()

    /** 相机角速度（视图空间，弧度/秒，**未缩放**） */
    @JvmField
    val omega: Vector3f = Vector3f()

    /** 相机角加速度（视图空间，弧度/秒²，**未缩放**） */
    @JvmField
    val alpha: Vector3f = Vector3f()
}

/**
 * 一帧解算需要的全部标量（都是**方块/秒**量纲）。
 *
 * 单独拎出来是为了让 [CharmSolver] 保持"无分配、无 MC 类型"：
 * 参数由 [CharmRuntime] 从配件数据 + 模型几何 + 当前状态（开镜进度）算好后填进来，
 * 每个 (手, 配件) 只保留一份、逐帧原地更新。
 */
class CharmParams {
    /** 摆长（方块），来自 `string` 分组的绑定包围盒高度 */
    @JvmField
    var length: Float = 0.02f

    /** 等效重力（方块/秒²） */
    @JvmField
    var gravity: Float = 9.8f

    /**
     * 速度阻尼（1/秒）。阻尼比 `ζ = damping / (2·2πf)`，小于 1 才会**振荡**：
     * 默认 4 配上 1.5Hz 约 0.21 —— 每摆一次衰减到 26%，停转/急停之后能来回荡三四下。
     * 20 以上基本"黏在枪上"，3 以下会像钟摆一样晃很久。
     *
     * ⚠ 积分方案本身**不再**贡献阻尼（见 [CharmSolver] 顶部"数值方案"里"扣掉径向"那一步），
     * 所以这个参数写多少就是多少。
     */
    @JvmField
    var damping: Float = 4.0f

    /**
     * 惯性响应（0..1），开镜时会被压小
     */
    @JvmField
    var response: Float = 0.15f

    /**
     * 转身响应（0..1，默认 0.30）
     */
    @JvmField
    var turnResponse: Float = 0.3f

    /**
     * 输入平滑时间常数（秒），0 = 关闭。
     *
     * 相机速度的低通与角速度的低通用的都是它；悬挂点那一侧在 `CharmRuntime` 里用同一个值。
     */
    @JvmField
    var smoothing: Float = 0.06f

    /**
     * 侧向限位：绳方向在**模型局部 X** 上的分量上限（`sin(角度)`，带符号，0 = 不限制）。
     *
     * 只在 [CharmRuntime] 换基到模型局部之后才用得上，解算本身在视图空间里跑，
     * 所以这个值不参与 [CharmSolver]。
     */
    @JvmField
    var lateralLimitSin: Float = 0f

    /**
     * **枪身侧**（[blockedAxisSign] 指向的那一侧）的最大摆角（弧度），默认 35°。
     *
     * ⚠ 它只限制**朝枪身这一侧**的偏离：偏离到别的方位（无遮挡侧、正前、正后）时上限换成
     * [maxAngleFree]，过渡是单边的（正前/正后**不在**中点，而是和最宽的一侧一样）。
     * 旧的对称圆锥只剩兜底作用（见 [CharmSolver.clampSwingAngle]）。
     */
    @JvmField
    var maxAngle: Float = 0.611f

    /**
     * [maxAngle] 的余弦。
     *
     * ⚠ 现在**解算器不再读它**（上限逐帧在变，只能比角度）。保留是为了不破坏外部引用
     * （离线测试台会读），[setMaxAngleDegrees] 仍会同步维护它。
     */
    @JvmField
    var maxAngleCos: Float = 0.819f

    /**
     * **非枪身侧**的最大摆角（弧度），默认 90°：无遮挡侧、正前、正后都用它。
     *
     * 只在 [blockedAxisSign] 不为 0（数据里写了 `LateralAngle`，即声明了哪一侧被枪身挡住）
     * 时生效 —— 没声明侧别时无从谈起"哪一侧更窄"，圆锥保持对称的旧行为。
     *
     * ⚠ 它是**动态过冲的天花板，不是平衡点**：接近它时软限位已经把回正力放大到
     * `1 + k` 倍，而驱动力 `∝cosθ` 还在变小，稳态解到不了这里（见 [CharmSolver.applySoftLimit]）。
     */
    @JvmField
    var maxAngleFree: Float = (Math.PI / 2).toFloat()

    /**
     * 软限位强度 `k`（默认 3，`0` = 关闭，行为回到只有硬夹逼的旧版）。
     *
     * 偏离角 `θ` 处的额外回正 = `k·(θ/上限)²` 倍的重力回正 —— 越偏离越硬，
     * 把质点减速在限位之前。上限别写太大：壁面附近的等效频率
     * `ω_eff ≈ sqrt(g/L · 2k/上限)`，`k` 越大越接近 Verlet 的稳定边界（`ω·dt < 2`）。
     */
    @JvmField
    var limitStiffness: Float = 3f

    /**
     * 哪一侧是"被枪身挡住的那一侧"：`+1` = 模型局部 **+X**（屏幕右，[lateralLimitSin] 为正时），
     * `-1` = −X，`0`（默认）= 没声明 —— 此时 [maxAngleFree] 不生效。
     */
    @JvmField
    var blockedAxisSign: Float = 0f

    /** 静止余摆幅度（弧度） */
    @JvmField
    var idleSway: Float = 0f

    fun setMaxAngleDegrees(degrees: Double) {
        val rad = Math.toRadians(degrees.coerceIn(0.0, 179.0)).toFloat()
        maxAngle = rad
        maxAngleCos = cos(rad)
    }

    /**
     * 无遮挡侧的上限（度）。会钳到 `[maxAngle, 179°]`：比枪身侧还小等于没有开侧，
     * 反而让过渡倒挂。
     *
     * ⚠ 必须在 [setMaxAngleDegrees] **之后**调用（它依赖 [maxAngle]）。
     */
    fun setMaxAngleFreeDegrees(degrees: Double) {
        val rad = Math.toRadians(degrees.coerceIn(0.0, 179.0)).toFloat()
        maxAngleFree = rad.coerceAtLeast(maxAngle)
    }

    /**
     * 等效重力 = `L · (2πf)²`。
     *
     * 这是 `ω² = g/L` 的反解：给定"想让它一秒摆几次"，不管摆长多少都能得到同样的观感。
     * 数据里同时写了 `Gravity` 与 `Frequency` 时以 `Frequency` 为准。
     */
    fun applyFrequency(frequency: Double) {
        val f = frequency.coerceAtLeast(MIN_FREQUENCY).toFloat()
        val omega = (2.0 * Math.PI * f).toFloat()
        gravity = length * omega * omega
    }

    companion object {
        /** 频率下限：低于它摆长再长也看不出摆动，且会让积分变得迟钝 */
        const val MIN_FREQUENCY: Double = 0.05
    }
}

/**
 * 相机运动量的**纯数学**部分。
 *
 * 单独拎出来是为了能脱离游戏跑自测（见 `build/verify` 里的临时测试台）：角速度的正负号
 * 完全靠矩阵约定推出来的，在这类代码里是重灾区，值得单独验一遍。
 */
object CharmFrameMath {

    private val scratchA = Matrix4f()
    private val scratchB = Matrix4f()
    private val scratchRot = Quaternionf()
    private val scratchAxis = Vector3f()

    /**
     * 由两帧的「**视图 ← 世界**」旋转差分出**视图空间**的角速度
     */
    @JvmStatic
    fun angularVelocity(viewFromWorldPrev: Matrix4f, viewFromWorldNow: Matrix4f, dt: Float, dest: Vector3f) {
        if (dt <= 0f) {
            dest.set(0f, 0f, 0f)
            return
        }
        // W_prev · W_nowᵀ
        scratchA.set(viewFromWorldPrev)
        scratchB.set(viewFromWorldNow).transpose()
        scratchA.mul(scratchB)
        scratchA.getUnnormalizedRotation(scratchRot)

        // θ·n̂ = 2·atan2(|v|, w) · v/|v|（joml 的 Quaternionf 没有现成的轴角取值：
        // `angle()` 走 acos，符号信息在 (x,y,z) 里，所以自己算更省事也更稳）
        val vx = scratchRot.x
        val vy = scratchRot.y
        val vz = scratchRot.z
        val len = sqrt(vx * vx + vy * vy + vz * vz)
        if (len < 1.0e-8f) {
            dest.set(0f, 0f, 0f)
            return
        }
        val angle = 2f * atan2(len, scratchRot.w())
        dest.set(vx, vy, vz).mul(angle / len / dt)
    }
}
