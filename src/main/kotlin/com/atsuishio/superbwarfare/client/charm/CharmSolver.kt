package com.atsuishio.superbwarfare.client.charm

import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.*

/**
 * 吊坠摆锤解算器。
 *
 * **纯数学，不引用任何 MC 类型**，方便单独跑曲线自测；几何量的采集与骨骼注入在
 * [CharmRuntime] 与 `BedrockAttachmentModel` 里。
 *
 * ## 参考系：相机原点 + 世界轴（**不是视图空间**）
 *
 * 质点位置存在「原点跟随相机平动、轴向恒为世界轴」的参考系里。这一点是整个方案的关键：
 *
 * | 参考系 | 视角转动时吊坠会不会滞后 | 玩家前进/后退会不会摆 |
 * |---|---|---|
 * | 视图空间（相机的**旋转**也一起跟） | ❌ 质点跟着相机转，看不出晃动 | 只有走路摇晃那点相对位移 |
 * | 相机原点 + 世界轴（本实现） | ✅ 悬挂点绕相机画弧，质点留在原地 | ✅ 见下 |
 *
 * 而且这个参考系**只有平移没有旋转**，于是：
 * - 重力恒为 `(0, -g, 0)`，不需要任何换算；
 * - Verlet 只依赖二阶差分，**对平移参考系不变** —— 玩家起步/急停时相机带着参考系一起加速，
 *   质点的"落后"会自动出现在**参考系内的相对位置**上（`bob - anchor` 往后倒）。
 *   需要显式补的只有**相机速度变化带来的伪力**（[update] 里的 `ddx/ddy/ddz`），
 *   它才是"起步走路时吊坠往后甩"里那个"起步"的物理来源。
 *
 * ## 数值方案
 *
 * Verlet 自由飞行 + **球面投影约束**（刚性摆杆，绳长恒定）：
 *
 * ```
 * v   = dA + (dR - dA) · response                  // 部分惯性
 * bob = bob + v·damp + g·dt² - response·Δ(camDelta) // 重力 + 相机伪力
 * bob = anchor + normalize(bob - anchor) · L        // 球面约束
 * ```
 *
 * 用**恒等长的球面**而不是绳子约束（`|d| > L` 才投影）是刻意的：模型里的 `string` 组是刚性网格，
 * 自己不会缩短，绳子一松就会看到本体穿进挂点。
 */
class CharmSolver {

    /** 挂件本体在参考系里的位置（方块） */
    private val bob = Vector3f()

    /** 上一帧的 [bob] */
    private val bobPrev = Vector3f()

    /** 上一帧的悬挂点（参考系内） */
    private val anchorPrev = Vector3f()

    /** 本帧的悬挂点（参考系内） */
    private val anchor = Vector3f()

    /** 上一帧的相机世界坐标（**double**：世界坐标可以很大，float 在大坐标下会丢掉帧间位移） */
    private val camPrev = DoubleArray(3)

    /**
     * 相机每步位移的一阶低通（相当于"平滑后的速度"）与上一步的值。
     *
     * ⚠ 为什么非低通不可：玩家坐标是**按 tick 插值**的折线，它的导数是阶梯状的，
     * 二阶差分因此在每个 tick 边界上打一个脉冲。直接拿它当惯性伪力，就是一串 20Hz 的踢 ——
     * 这正是走路/飞行时吊坠高频抖动的来源。低通之后再差分，剩下的才是"真在加速"那部分。
     */
    private val camVelocity = DoubleArray(3)
    private val camVelocityPrev = DoubleArray(3)

    /** 相机速度基线是否建立；`false` 时这一步不施加伪力（见 [update]） */
    private var camReady = false

    /** 参考系内的绳向量（单位向量，指向挂件） */
    val rope = Vector3f(0f, -1f, 0f)

    /** 静止余摆的相位（秒） */
    private var swayTime = 0f

    private var initialized = false

    /** 是否已经有过一次有效推进 */
    val isReady: Boolean get() = initialized

    /** 把质点直接放到"悬挂点正下方"，并清空全部速度 */
    fun reset(anchorIn: Vector3f, length: Float) {
        anchor.set(anchorIn)
        anchorPrev.set(anchorIn)
        bob.set(anchorIn.x, anchorIn.y - length, anchorIn.z)
        bobPrev.set(bob)
        rope.set(0f, -1f, 0f)
        swayTime = 0f
        camReady = false
        initialized = true
    }

    /** 坐标系换了（换手/换枪/长时间没渲染）时用它，下一帧 [update] 会重新归位 */
    fun invalidate() {
        initialized = false
    }

    /**
     * 推进一帧。
     *
     * @param anchorIn 悬挂点在参考系里的位置（方块）。
     * @param camX/camY/camZ 相机**世界坐标**，只用来算帧间位移的差分（伪力）。
     * @param dt 本帧时长（秒），调用方已夹逼过上下限。
     * @return `true` = 这一帧真的推进了；`false` = 只是（重新）归位，画绑定姿态即可。
     */
    fun update(
        anchorIn: Vector3f,
        camX: Double,
        camY: Double,
        camZ: Double,
        p: CharmParams,
        dt: Float,
    ): Boolean {
        if (!initialized) {
            reset(anchorIn, p.length)
            rememberCamera(camX, camY, camZ)
            return false
        }

        // 悬挂点单帧跳变过大（换枪、绘制动画、传送、副武器接管……）：直接归位。
        // 不这么做的话，那一下位移会在约束里被折算成一个巨大的切向速度，
        // 吊坠会像被弹弓打出去一样飞起来。
        if (anchorPrev.distanceSquared(anchorIn) > MAX_ANCHOR_JUMP * MAX_ANCHOR_JUMP) {
            reset(anchorIn, p.length)
            rememberCamera(camX, camY, camZ)
            return false
        }

        // 相机在这一步里的世界位移；先低通成"平滑速度"，再差分得到真正的加速度。
        // （直接差分原始位移就是 20Hz 脉冲串，见 [camVelocity] 的说明）
        val dx = camX - camPrev[0]
        val dy = camY - camPrev[1]
        val dz = camZ - camPrev[2]
        val camDdx: Float
        val camDdy: Float
        val camDdz: Float
        if (camReady) {
            val k = if (p.smoothing > 0f) 1.0 - exp(-dt / p.smoothing) else 1.0
            camVelocity[0] += (dx - camVelocity[0]) * k
            camVelocity[1] += (dy - camVelocity[1]) * k
            camVelocity[2] += (dz - camVelocity[2]) * k
            camDdx = (camVelocity[0] - camVelocityPrev[0]).toFloat()
            camDdy = (camVelocity[1] - camVelocityPrev[1]).toFloat()
            camDdz = (camVelocity[2] - camVelocityPrev[2]).toFloat()
        } else {
            // 刚归位：不知道上一刻的速度，就把当前位移直接当基线 —— 伪力这一脚先记 0，
            // 否则"走动中被重置"会凭空吃到一次和走路速度等量的踢。
            camVelocity[0] = dx
            camVelocity[1] = dy
            camVelocity[2] = dz
            camDdx = 0f
            camDdy = 0f
            camDdz = 0f
            camReady = true
        }
        camVelocityPrev[0] = camVelocity[0]
        camVelocityPrev[1] = camVelocity[1]
        camVelocityPrev[2] = camVelocity[2]

        anchor.set(anchorIn)

        // 部分惯性：response = 0 → 速度完全取悬挂点的位移（质点被枪"拎着走"），
        // response = 1 → 速度取质点自己的历史位移（完全物理）。
        val response = p.response
        val dRx = bob.x - bobPrev.x
        val dRy = bob.y - bobPrev.y
        val dRz = bob.z - bobPrev.z
        val dAx = anchor.x - anchorPrev.x
        val dAy = anchor.y - anchorPrev.y
        val dAz = anchor.z - anchorPrev.z

        val damp = exp(-p.damping * dt)
        val vx = (dAx + (dRx - dAx) * response) * damp
        val vy = (dAy + (dRy - dAy) * response) * damp
        val vz = (dAz + (dRz - dAz) * response) * damp

        bobPrev.set(bob)
        // ⚠ 悬挂点的"上一帧位置"也必须推进：下一帧的 `dA`（悬挂点位移）与跳变检测都读它，
        // 忘了写就会拿一个越来越旧的锚点去算 —— 部分惯性整项失效、跳变检测还会误报。
        anchorPrev.set(anchor)

        val gt2 = p.gravity * dt * dt
        bob.set(
            bob.x + vx - response * camDdx,
            bob.y + vy - gt2 - response * camDdy,
            bob.z + vz - response * camDdz,
        )

        projectToRope(p)
        clampSwingAngle(p)
        buildRope()
        applyIdleSway(p, dt)

        rememberCamera(camX, camY, camZ)
        return true
    }

    /** 球面约束：把质点拉回"以悬挂点为球心、摆长为半径"的球面上 */
    private fun projectToRope(p: CharmParams) {
        val ox = bob.x - anchor.x
        val oy = bob.y - anchor.y
        val oz = bob.z - anchor.z
        val distSq = ox * ox + oy * oy + oz * oz

        if (distSq < EPSILON) {
            // 退化：质点正好落在悬挂点上，随便给它一个方向（正下方）
            bob.set(anchor.x, anchor.y - p.length, anchor.z)
            return
        }

        val k = p.length / sqrt(distSq)
        bob.set(anchor.x + ox * k, anchor.y + oy * k, anchor.z + oz * k)
    }

    /**
     * 以世界下方为轴的锥形夹逼。
     *
     * 两个作用：不让吊坠甩进枪身/手臂里；以及让方向**永远远离"与正下方相反"那一点** ——
     * 最短弧旋转（[Quaternionf.rotationTo]）在那一处会因为叉积为零而翻面。
     */
    private fun clampSwingAngle(p: CharmParams) {
        val ox = bob.x - anchor.x
        val oy = bob.y - anchor.y
        val oz = bob.z - anchor.z
        val len = sqrt(ox * ox + oy * oy + oz * oz)
        if (len < EPSILON) return

        val nx = ox / len
        val ny = oy / len
        val nz = oz / len

        // 与世界下方 (0,-1,0) 的夹角余弦
        val cos = -ny
        if (cos >= p.maxAngleCos) return

        // 旋转轴 = dir × (0,-1,0) = (nz, 0, -nx)
        var ax = nz
        var az = -nx
        val axisLen = sqrt(ax * ax + az * az)
        if (axisLen < EPSILON) {
            // 方向几乎正对上方：轴任取一个与下方垂直的方向
            ax = 1f
            az = 0f
        } else {
            ax /= axisLen
            az /= axisLen
        }

        val excess = acos(cos.coerceIn(-1f, 1f)) - p.maxAngle
        if (excess <= 0f) return

        val q = Quaternionf().fromAxisAngleRad(ax, 0f, az, excess)
        val dir = q.transform(Vector3f(nx, ny, nz))
        bob.set(anchor.x + dir.x * p.length, anchor.y + dir.y * p.length, anchor.z + dir.z * p.length)
    }

    /** 由质点位置得到绳向量（单位向量，指向挂件） */
    private fun buildRope() {
        rope.set(bob.x - anchor.x, bob.y - anchor.y, bob.z - anchor.z)
        if (rope.lengthSquared() < EPSILON) {
            rope.set(0f, -1f, 0f)
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

    /** 记下这一步的相机世界坐标，供下一步算帧间位移（速度的低通状态另外维护） */
    private fun rememberCamera(x: Double, y: Double, z: Double) {
        camPrev[0] = x
        camPrev[1] = y
        camPrev[2] = z
    }

    companion object {
        private const val EPSILON = 1.0e-9f

        /** 单帧悬挂点位移超过这个距离（方块）就判定为不连续 */
        private const val MAX_ANCHOR_JUMP = 0.6f

        private const val SWAY_SPEED_X = 0.9f
        private const val SWAY_SPEED_Z = 0.63f
        private const val SWAY_PHASE_Z = 1.3f
    }
}

/**
 * 一帧解算需要的全部标量（都是**方块/秒**量纲）。
 *
 * 单独拎出来是为了让 [CharmSolver] 保持"无分配、无 MC 类型"：
 * 参数由 [CharmRuntime] 从配件数据 + 模型几何 + 当前状态（开镜进度）算好后填进来，
 * 每个 (手, 配件) 只保留一份、逐帧原地更新。
 */
class CharmParams {
    /** 摆长（方块），来自 `string` 骨骼自身的 Y 偏移 */
    @JvmField
    var length: Float = 0.02f

    /** 等效重力（方块/秒²） */
    @JvmField
    var gravity: Float = 9.8f

    /** 速度阻尼（1/秒） */
    @JvmField
    var damping: Float = 6.0f

    /** 惯性响应（0..1），开镜时会被压小 */
    @JvmField
    var response: Float = 0.15f

    /**
     * 输入平滑时间常数（秒），0 = 关闭。
     *
     * 相机位移的低通用的就是它；悬挂点那一侧在 `CharmRuntime` 里用同一个值。
     */
    @JvmField
    var smoothing: Float = 0.06f

    /**
     * 侧向限位：绳方向在**模型局部 X** 上的分量上限（`sin(角度)`，带符号，0 = 不限制）。
     *
     * 只在 [CharmRuntime] 换基到模型局部之后才用得上，解算本身在重力参考系里跑，
     * 所以这个值不参与 [CharmSolver]。
     */
    @JvmField
    var lateralLimitSin: Float = 0f

    /** 最大摆角（弧度） */
    @JvmField
    var maxAngle: Float = 0.611f

    /** [maxAngle] 的余弦，夹逼时直接比较，省一次 `acos` */
    @JvmField
    var maxAngleCos: Float = 0.819f

    /** 静止余摆幅度（弧度） */
    @JvmField
    var idleSway: Float = 0f

    fun setMaxAngleDegrees(degrees: Double) {
        val rad = Math.toRadians(degrees.coerceIn(0.0, 179.0)).toFloat()
        maxAngle = rad
        maxAngleCos = cos(rad)
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
